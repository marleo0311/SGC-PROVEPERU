ALTER TABLE comprobante DROP CONSTRAINT ck_comprobante_estado;
ALTER TABLE comprobante ADD CONSTRAINT ck_comprobante_estado CHECK (
    estado IN ('EMITIDO', 'ANULADO', 'PENDIENTE_ENVIO', 'BAJA_PENDIENTE')
);

CREATE SEQUENCE comunicacion_baja_correlativo_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE comunicacion_baja_sunat (
    id_comunicacion_baja BIGSERIAL PRIMARY KEY,
    id_comprobante BIGINT NOT NULL,
    id_usuario BIGINT NOT NULL,
    ambiente VARCHAR(20) NOT NULL,
    fecha_documento DATE NOT NULL,
    fecha_generacion DATE NOT NULL,
    correlativo INTEGER NOT NULL,
    motivo VARCHAR(300) NOT NULL,
    estado VARCHAR(40) NOT NULL,
    nombre_archivo VARCHAR(120) NOT NULL,
    hash_xml VARCHAR(64) NOT NULL,
    xml_firmado BYTEA NOT NULL,
    zip_enviado BYTEA NOT NULL,
    cdr_zip BYTEA,
    ticket VARCHAR(120),
    codigo_estado_ticket VARCHAR(20),
    codigo_respuesta VARCHAR(20),
    descripcion_respuesta VARCHAR(1000),
    observaciones TEXT,
    error_ultimo VARCHAR(2000),
    intentos_envio INTEGER NOT NULL DEFAULT 0,
    consultas_estado INTEGER NOT NULL DEFAULT 0,
    fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_ultimo_intento TIMESTAMPTZ,
    fecha_ultima_consulta TIMESTAMPTZ,
    fecha_respuesta TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_comunicacion_baja_comprobante FOREIGN KEY (id_comprobante)
        REFERENCES comprobante (id_comprobante),
    CONSTRAINT fk_comunicacion_baja_usuario FOREIGN KEY (id_usuario)
        REFERENCES usuario (id_usuario),
    CONSTRAINT uq_comunicacion_baja_comprobante_ambiente UNIQUE (id_comprobante, ambiente),
    CONSTRAINT uq_comunicacion_baja_identificador UNIQUE (ambiente, fecha_generacion, correlativo),
    CONSTRAINT ck_comunicacion_baja_ambiente CHECK (ambiente IN ('BETA', 'PRODUCCION')),
    CONSTRAINT ck_comunicacion_baja_estado CHECK (estado IN (
        'GENERADO', 'ENVIANDO', 'TICKET_RECIBIDO', 'PROCESANDO',
        'ACEPTADO', 'ACEPTADO_CON_OBSERVACIONES', 'RECHAZADO', 'ERROR_COMUNICACION'
    )),
    CONSTRAINT ck_comunicacion_baja_intentos CHECK (intentos_envio >= 0),
    CONSTRAINT ck_comunicacion_baja_consultas CHECK (consultas_estado >= 0)
);

CREATE INDEX idx_comunicacion_baja_estado
    ON comunicacion_baja_sunat (estado, fecha_creacion DESC);

INSERT INTO permiso (codigo, nombre, modulo, descripcion)
VALUES (
    'VEN_SUNAT_BAJAS_GESTIONAR',
    'Gestionar bajas electrónicas SUNAT',
    'Comprobantes',
    'Solicitar, enviar y consultar comunicaciones de baja y anulaciones de boletas'
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
  AND p.codigo = 'VEN_SUNAT_BAJAS_GESTIONAR'
ON CONFLICT DO NOTHING;
