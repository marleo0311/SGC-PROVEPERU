CREATE SEQUENCE nota_credito_numero_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE nota_debito_numero_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE nota_electronica (
    id_nota_electronica BIGSERIAL PRIMARY KEY,
    id_comprobante_origen BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    serie VARCHAR(4) NOT NULL,
    numero VARCHAR(8) NOT NULL,
    codigo_motivo VARCHAR(2) NOT NULL,
    descripcion_motivo VARCHAR(300) NOT NULL,
    fecha_emision TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    subtotal NUMERIC(14,2) NOT NULL,
    igv NUMERIC(14,2) NOT NULL,
    total NUMERIC(14,2) NOT NULL,
    id_usuario BIGINT NOT NULL,
    ambiente VARCHAR(20) NOT NULL,
    estado VARCHAR(40) NOT NULL,
    nombre_archivo VARCHAR(120) NOT NULL,
    hash_xml VARCHAR(64) NOT NULL,
    xml_firmado BYTEA NOT NULL,
    zip_enviado BYTEA NOT NULL,
    cdr_zip BYTEA,
    codigo_respuesta VARCHAR(20),
    descripcion_respuesta VARCHAR(1000),
    observaciones TEXT,
    error_ultimo VARCHAR(2000),
    intentos INTEGER NOT NULL DEFAULT 0,
    fecha_ultimo_intento TIMESTAMPTZ,
    fecha_respuesta TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_nota_electronica_comprobante FOREIGN KEY (id_comprobante_origen)
        REFERENCES comprobante (id_comprobante),
    CONSTRAINT fk_nota_electronica_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario),
    CONSTRAINT uq_nota_electronica_numero UNIQUE (tipo, serie, numero),
    CONSTRAINT ck_nota_electronica_tipo CHECK (tipo IN ('CREDITO', 'DEBITO')),
    CONSTRAINT ck_nota_electronica_ambiente CHECK (ambiente IN ('BETA', 'PRODUCCION')),
    CONSTRAINT ck_nota_electronica_estado CHECK (estado IN (
        'GENERADO', 'ENVIANDO', 'ACEPTADO', 'ACEPTADO_CON_OBSERVACIONES',
        'RECHAZADO', 'ERROR_COMUNICACION'
    )),
    CONSTRAINT ck_nota_electronica_importes CHECK (
        subtotal >= 0 AND igv >= 0 AND total > 0 AND subtotal + igv = total
    ),
    CONSTRAINT ck_nota_electronica_intentos CHECK (intentos >= 0)
);

CREATE INDEX idx_nota_electronica_origen_fecha
    ON nota_electronica (id_comprobante_origen, fecha_emision DESC);
CREATE INDEX idx_nota_electronica_estado
    ON nota_electronica (estado, fecha_ultimo_intento DESC);

INSERT INTO permiso (codigo, nombre, modulo, descripcion)
VALUES (
    'VEN_SUNAT_NOTAS_GESTIONAR',
    'Gestionar notas electrónicas SUNAT',
    'Comprobantes',
    'Emitir, enviar y descargar notas de crédito y débito electrónicas'
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
  AND p.codigo = 'VEN_SUNAT_NOTAS_GESTIONAR'
ON CONFLICT DO NOTHING;
