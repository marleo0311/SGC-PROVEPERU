ALTER TABLE empresa
    ADD COLUMN ubigeo VARCHAR(6),
    ADD COLUMN departamento VARCHAR(80),
    ADD COLUMN provincia VARCHAR(80),
    ADD COLUMN distrito VARCHAR(80),
    ADD COLUMN codigo_pais VARCHAR(2) NOT NULL DEFAULT 'PE';

ALTER TABLE empresa
    ADD CONSTRAINT ck_empresa_ubigeo_formato CHECK (
        ubigeo IS NULL OR ubigeo ~ '^[0-9]{6}$'
    ),
    ADD CONSTRAINT ck_empresa_codigo_pais_formato CHECK (
        codigo_pais ~ '^[A-Z]{2}$'
    );

ALTER TABLE unidad_medida
    ADD COLUMN codigo_sunat VARCHAR(3) NOT NULL DEFAULT 'NIU';

ALTER TABLE unidad_medida
    ADD CONSTRAINT ck_unidad_medida_codigo_sunat CHECK (
        codigo_sunat ~ '^[A-Z0-9]{3}$'
    );

CREATE TABLE envio_sunat (
    id_envio_sunat BIGSERIAL PRIMARY KEY,
    id_comprobante BIGINT NOT NULL,
    ambiente VARCHAR(20) NOT NULL,
    estado VARCHAR(40) NOT NULL,
    nombre_archivo VARCHAR(120) NOT NULL,
    hash_xml VARCHAR(64) NOT NULL,
    xml_firmado BYTEA NOT NULL,
    zip_enviado BYTEA NOT NULL,
    cdr_zip BYTEA,
    ticket VARCHAR(120),
    codigo_respuesta VARCHAR(20),
    descripcion_respuesta VARCHAR(1000),
    observaciones TEXT,
    error_ultimo VARCHAR(2000),
    intentos INTEGER NOT NULL DEFAULT 0,
    fecha_generacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_ultimo_intento TIMESTAMPTZ,
    fecha_respuesta TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_envio_sunat_comprobante
        FOREIGN KEY (id_comprobante) REFERENCES comprobante (id_comprobante),
    CONSTRAINT uq_envio_sunat_comprobante UNIQUE (id_comprobante),
    CONSTRAINT ck_envio_sunat_ambiente CHECK (
        ambiente IN ('BETA', 'PRODUCCION')
    ),
    CONSTRAINT ck_envio_sunat_estado CHECK (
        estado IN (
            'GENERADO',
            'ENVIANDO',
            'ACEPTADO',
            'ACEPTADO_CON_OBSERVACIONES',
            'RECHAZADO',
            'ERROR_COMUNICACION'
        )
    ),
    CONSTRAINT ck_envio_sunat_intentos CHECK (intentos >= 0)
);

CREATE INDEX idx_envio_sunat_estado_fecha
    ON envio_sunat (estado, fecha_ultimo_intento DESC);

INSERT INTO permiso (codigo, nombre, modulo, descripcion)
VALUES (
    'VEN_SUNAT_ENVIAR',
    'Enviar comprobantes a SUNAT',
    'Comprobantes',
    'Generar, firmar y enviar comprobantes electrónicos a SUNAT'
)
ON CONFLICT (codigo) DO UPDATE SET
    nombre = EXCLUDED.nombre,
    modulo = EXCLUDED.modulo,
    descripcion = EXCLUDED.descripcion;

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r
CROSS JOIN permiso p
WHERE lower(r.nombre) = lower('Administrador')
  AND p.codigo = 'VEN_SUNAT_ENVIAR'
ON CONFLICT DO NOTHING;
