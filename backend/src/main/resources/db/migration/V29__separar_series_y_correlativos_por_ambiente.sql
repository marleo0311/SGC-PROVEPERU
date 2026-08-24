ALTER TABLE comprobante
    ADD COLUMN ambiente VARCHAR(20) NOT NULL DEFAULT 'BETA';

ALTER TABLE comprobante
    ADD CONSTRAINT ck_comprobante_ambiente CHECK (
        ambiente IN ('BETA', 'PRODUCCION')
    );

ALTER TABLE comprobante
    DROP CONSTRAINT uq_comprobante_serie_numero;

ALTER TABLE comprobante
    ADD CONSTRAINT uq_comprobante_ambiente_serie_numero
        UNIQUE (ambiente, serie, numero);

ALTER TABLE nota_electronica
    DROP CONSTRAINT uq_nota_electronica_numero;

ALTER TABLE nota_electronica
    ADD CONSTRAINT uq_nota_electronica_ambiente_numero
        UNIQUE (ambiente, tipo, serie, numero);

ALTER TABLE sede
    ADD CONSTRAINT uq_sede_id_empresa UNIQUE (id_sede, id_empresa);

CREATE TABLE serie_comprobante (
    id_serie_comprobante BIGSERIAL PRIMARY KEY,
    id_empresa BIGINT NOT NULL,
    id_sede BIGINT NOT NULL,
    ambiente VARCHAR(20) NOT NULL,
    tipo_documento VARCHAR(40) NOT NULL,
    serie VARCHAR(4) NOT NULL,
    ultimo_correlativo BIGINT NOT NULL DEFAULT 0,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_serie_comprobante_empresa
        FOREIGN KEY (id_empresa) REFERENCES empresa (id_empresa),
    CONSTRAINT fk_serie_comprobante_sede
        FOREIGN KEY (id_sede, id_empresa) REFERENCES sede (id_sede, id_empresa),
    CONSTRAINT uq_serie_comprobante_numero
        UNIQUE (id_empresa, ambiente, tipo_documento, serie),
    CONSTRAINT ck_serie_comprobante_ambiente CHECK (
        ambiente IN ('BETA', 'PRODUCCION')
    ),
    CONSTRAINT ck_serie_comprobante_tipo CHECK (
        tipo_documento IN (
            'BOLETA',
            'FACTURA',
            'NOTA_CREDITO_BOLETA',
            'NOTA_DEBITO_BOLETA',
            'NOTA_CREDITO_FACTURA',
            'NOTA_DEBITO_FACTURA'
        )
    ),
    CONSTRAINT ck_serie_comprobante_formato CHECK (
        serie ~ '^[BF][A-Z0-9]{3}$'
    ),
    CONSTRAINT ck_serie_comprobante_correlativo CHECK (
        ultimo_correlativo BETWEEN 0 AND 99999999
    )
);

CREATE UNIQUE INDEX uq_serie_comprobante_activa_sede_tipo
    ON serie_comprobante (id_sede, ambiente, tipo_documento)
    WHERE activo;

CREATE INDEX idx_serie_comprobante_empresa_ambiente
    ON serie_comprobante (id_empresa, ambiente, activo);

INSERT INTO serie_comprobante (
    id_empresa,
    id_sede,
    ambiente,
    tipo_documento,
    serie,
    ultimo_correlativo,
    activo
)
SELECT
    s.id_empresa,
    s.id_sede,
    'BETA',
    c.tipo,
    c.serie,
    MAX(c.numero::BIGINT),
    TRUE
FROM comprobante c
JOIN venta v ON v.id_venta = c.id_venta
JOIN sede s ON s.id_sede = v.id_sede
WHERE c.tipo IN ('BOLETA', 'FACTURA')
GROUP BY s.id_empresa, s.id_sede, c.tipo, c.serie
ON CONFLICT (id_empresa, ambiente, tipo_documento, serie)
DO UPDATE SET
    ultimo_correlativo = GREATEST(
        serie_comprobante.ultimo_correlativo,
        EXCLUDED.ultimo_correlativo
    ),
    fecha_actualizacion = CURRENT_TIMESTAMP;

INSERT INTO serie_comprobante (
    id_empresa,
    id_sede,
    ambiente,
    tipo_documento,
    serie,
    ultimo_correlativo,
    activo
)
SELECT
    s.id_empresa,
    s.id_sede,
    n.ambiente,
    CASE
        WHEN n.tipo = 'CREDITO' AND c.tipo = 'BOLETA'
            THEN 'NOTA_CREDITO_BOLETA'
        WHEN n.tipo = 'DEBITO' AND c.tipo = 'BOLETA'
            THEN 'NOTA_DEBITO_BOLETA'
        WHEN n.tipo = 'CREDITO' AND c.tipo = 'FACTURA'
            THEN 'NOTA_CREDITO_FACTURA'
        ELSE 'NOTA_DEBITO_FACTURA'
    END,
    n.serie,
    MAX(n.numero::BIGINT),
    TRUE
FROM nota_electronica n
JOIN comprobante c ON c.id_comprobante = n.id_comprobante_origen
JOIN venta v ON v.id_venta = c.id_venta
JOIN sede s ON s.id_sede = v.id_sede
GROUP BY s.id_empresa, s.id_sede, n.ambiente, n.tipo, c.tipo, n.serie
ON CONFLICT (id_empresa, ambiente, tipo_documento, serie)
DO UPDATE SET
    ultimo_correlativo = GREATEST(
        serie_comprobante.ultimo_correlativo,
        EXCLUDED.ultimo_correlativo
    ),
    fecha_actualizacion = CURRENT_TIMESTAMP;

WITH sede_principal AS (
    SELECT DISTINCT ON (id_empresa)
        id_empresa,
        id_sede
    FROM sede
    WHERE estado = 'ACTIVO'
    ORDER BY id_empresa, id_sede
),
series_iniciales (ambiente, tipo_documento, serie) AS (
    VALUES
        ('BETA', 'BOLETA', 'B001'),
        ('BETA', 'FACTURA', 'F001'),
        ('BETA', 'NOTA_CREDITO_BOLETA', 'BC01'),
        ('BETA', 'NOTA_DEBITO_BOLETA', 'BD01'),
        ('BETA', 'NOTA_CREDITO_FACTURA', 'FC01'),
        ('BETA', 'NOTA_DEBITO_FACTURA', 'FD01'),
        ('PRODUCCION', 'BOLETA', 'B001'),
        ('PRODUCCION', 'FACTURA', 'F001'),
        ('PRODUCCION', 'NOTA_CREDITO_BOLETA', 'BC01'),
        ('PRODUCCION', 'NOTA_DEBITO_BOLETA', 'BD01'),
        ('PRODUCCION', 'NOTA_CREDITO_FACTURA', 'FC01'),
        ('PRODUCCION', 'NOTA_DEBITO_FACTURA', 'FD01')
)
INSERT INTO serie_comprobante (
    id_empresa,
    id_sede,
    ambiente,
    tipo_documento,
    serie,
    ultimo_correlativo,
    activo
)
SELECT
    sp.id_empresa,
    sp.id_sede,
    si.ambiente,
    si.tipo_documento,
    si.serie,
    0,
    TRUE
FROM sede_principal sp
CROSS JOIN series_iniciales si
ON CONFLICT (id_empresa, ambiente, tipo_documento, serie) DO NOTHING;

COMMENT ON TABLE serie_comprobante IS
    'Series y correlativos atómicos separados por empresa, sede, ambiente y tipo documental.';
COMMENT ON COLUMN comprobante.ambiente IS
    'Ambiente asignado al emitir el comprobante; evita reenviarlo a un ambiente diferente.';
