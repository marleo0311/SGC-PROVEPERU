ALTER TABLE venta
    DROP CONSTRAINT ck_venta_comprobante;

ALTER TABLE venta
    ADD CONSTRAINT ck_venta_comprobante CHECK (
        tipo_comprobante IN ('NOTA_VENTA', 'BOLETA', 'FACTURA')
    );

CREATE SEQUENCE comprobante_nota_venta_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE comprobante_boleta_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE comprobante_factura_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE comprobante (
    id_comprobante BIGSERIAL PRIMARY KEY,
    id_venta BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    serie VARCHAR(20) NOT NULL,
    numero VARCHAR(30) NOT NULL,
    fecha_emision TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    subtotal NUMERIC(14,2) NOT NULL,
    igv NUMERIC(14,2) NOT NULL,
    total NUMERIC(14,2) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'EMITIDO',
    respuesta_sunat JSONB,
    fecha_anulacion TIMESTAMPTZ,
    motivo_anulacion VARCHAR(300),
    id_usuario_anulacion BIGINT,

    CONSTRAINT fk_comprobante_venta
        FOREIGN KEY (id_venta) REFERENCES venta (id_venta),
    CONSTRAINT fk_comprobante_usuario_anulacion
        FOREIGN KEY (id_usuario_anulacion) REFERENCES usuario (id_usuario),
    CONSTRAINT uq_comprobante_venta UNIQUE (id_venta),
    CONSTRAINT uq_comprobante_serie_numero UNIQUE (serie, numero),
    CONSTRAINT ck_comprobante_tipo CHECK (
        tipo IN ('NOTA_VENTA', 'BOLETA', 'FACTURA')
    ),
    CONSTRAINT ck_comprobante_importes CHECK (
        subtotal >= 0
        AND igv >= 0
        AND total = subtotal + igv
        AND total > 0
    ),
    CONSTRAINT ck_comprobante_estado CHECK (
        estado IN ('EMITIDO', 'ANULADO', 'PENDIENTE_ENVIO')
    ),
    CONSTRAINT ck_comprobante_anulacion CHECK (
        (estado = 'ANULADO'
            AND fecha_anulacion IS NOT NULL
            AND motivo_anulacion IS NOT NULL)
        OR (estado <> 'ANULADO'
            AND fecha_anulacion IS NULL
            AND motivo_anulacion IS NULL
            AND id_usuario_anulacion IS NULL)
    )
);

CREATE INDEX idx_comprobante_fecha
    ON comprobante (fecha_emision DESC);
CREATE INDEX idx_comprobante_tipo_fecha
    ON comprobante (tipo, fecha_emision DESC);
CREATE INDEX idx_comprobante_estado_fecha
    ON comprobante (estado, fecha_emision DESC);

INSERT INTO comprobante (
    id_venta,
    tipo,
    serie,
    numero,
    fecha_emision,
    subtotal,
    igv,
    total,
    estado,
    fecha_anulacion,
    motivo_anulacion
)
SELECT
    v.id_venta,
    v.tipo_comprobante,
    CASE v.tipo_comprobante
        WHEN 'BOLETA' THEN 'B001'
        WHEN 'FACTURA' THEN 'F001'
        ELSE 'NV01'
    END,
    lpad(v.id_venta::text, 8, '0'),
    v.fecha_hora,
    v.subtotal,
    v.igv,
    v.total,
    CASE WHEN v.estado = 'ANULADA' THEN 'ANULADO' ELSE 'EMITIDO' END,
    v.fecha_anulacion,
    v.motivo_anulacion
FROM venta v
ON CONFLICT (id_venta) DO NOTHING;

SELECT setval(
    'comprobante_nota_venta_seq',
    GREATEST(
        COALESCE((
            SELECT MAX(numero::BIGINT)
            FROM comprobante
            WHERE tipo = 'NOTA_VENTA'
        ), 0),
        1
    ),
    EXISTS (SELECT 1 FROM comprobante WHERE tipo = 'NOTA_VENTA')
);

SELECT setval(
    'comprobante_boleta_seq',
    GREATEST(
        COALESCE((
            SELECT MAX(numero::BIGINT)
            FROM comprobante
            WHERE tipo = 'BOLETA'
        ), 0),
        1
    ),
    EXISTS (SELECT 1 FROM comprobante WHERE tipo = 'BOLETA')
);

SELECT setval(
    'comprobante_factura_seq',
    GREATEST(
        COALESCE((
            SELECT MAX(numero::BIGINT)
            FROM comprobante
            WHERE tipo = 'FACTURA'
        ), 0),
        1
    ),
    EXISTS (SELECT 1 FROM comprobante WHERE tipo = 'FACTURA')
);

INSERT INTO permiso (codigo, nombre, modulo, descripcion)
VALUES
    ('VEN_COMPROBANTES_VER', 'Consultar comprobantes', 'Comprobantes', 'Consultar comprobantes y su representación imprimible'),
    ('VEN_COMPROBANTES_ANULAR', 'Anular comprobantes', 'Comprobantes', 'Anular el comprobante y la venta relacionada conservando su trazabilidad')
ON CONFLICT (codigo) DO UPDATE SET
    nombre = EXCLUDED.nombre,
    modulo = EXCLUDED.modulo,
    descripcion = EXCLUDED.descripcion;

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT r.id_rol, p.id_permiso
FROM rol r
CROSS JOIN permiso p
WHERE lower(r.nombre) = lower('Administrador')
  AND p.codigo IN (
      'VEN_COMPROBANTES_VER',
      'VEN_COMPROBANTES_ANULAR'
  )
ON CONFLICT DO NOTHING;
