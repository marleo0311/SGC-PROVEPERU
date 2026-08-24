CREATE TABLE correlativo_resumen_diario_sunat (
    ambiente VARCHAR(20) NOT NULL,
    fecha_documentos DATE NOT NULL,
    ultimo INTEGER NOT NULL,

    CONSTRAINT pk_correlativo_resumen_diario_sunat
        PRIMARY KEY (ambiente, fecha_documentos),
    CONSTRAINT ck_correlativo_resumen_ambiente CHECK (
        ambiente IN ('BETA', 'PRODUCCION')
    ),
    CONSTRAINT ck_correlativo_resumen_ultimo CHECK (ultimo > 0)
);

CREATE TABLE resumen_diario_sunat (
    id_resumen_diario_sunat BIGSERIAL PRIMARY KEY,
    ambiente VARCHAR(20) NOT NULL,
    fecha_documentos DATE NOT NULL,
    fecha_generacion DATE NOT NULL,
    correlativo INTEGER NOT NULL,
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

    CONSTRAINT uq_resumen_diario_sunat_identificador
        UNIQUE (ambiente, fecha_documentos, correlativo),
    CONSTRAINT ck_resumen_diario_sunat_ambiente CHECK (
        ambiente IN ('BETA', 'PRODUCCION')
    ),
    CONSTRAINT ck_resumen_diario_sunat_estado CHECK (
        estado IN (
            'GENERADO',
            'ENVIANDO',
            'TICKET_RECIBIDO',
            'PROCESANDO',
            'ACEPTADO',
            'ACEPTADO_CON_OBSERVACIONES',
            'RECHAZADO',
            'ERROR_COMUNICACION'
        )
    ),
    CONSTRAINT ck_resumen_diario_sunat_correlativo CHECK (correlativo > 0),
    CONSTRAINT ck_resumen_diario_sunat_intentos CHECK (intentos_envio >= 0),
    CONSTRAINT ck_resumen_diario_sunat_consultas CHECK (consultas_estado >= 0)
);

CREATE TABLE resumen_diario_sunat_item (
    id_resumen_diario_sunat BIGINT NOT NULL,
    id_comprobante BIGINT NOT NULL,

    CONSTRAINT pk_resumen_diario_sunat_item
        PRIMARY KEY (id_resumen_diario_sunat, id_comprobante),
    CONSTRAINT fk_resumen_item_resumen
        FOREIGN KEY (id_resumen_diario_sunat)
        REFERENCES resumen_diario_sunat (id_resumen_diario_sunat),
    CONSTRAINT fk_resumen_item_comprobante
        FOREIGN KEY (id_comprobante)
        REFERENCES comprobante (id_comprobante)
);

CREATE INDEX idx_resumen_diario_sunat_fecha_estado
    ON resumen_diario_sunat (fecha_documentos DESC, estado);

CREATE INDEX idx_resumen_diario_sunat_item_comprobante
    ON resumen_diario_sunat_item (id_comprobante);

INSERT INTO permiso (codigo, nombre, modulo, descripcion)
VALUES (
    'VEN_SUNAT_RESUMENES_GESTIONAR',
    'Gestionar resúmenes diarios SUNAT',
    'Comprobantes',
    'Generar, enviar y consultar el ticket de los resúmenes diarios de boletas'
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
  AND p.codigo = 'VEN_SUNAT_RESUMENES_GESTIONAR'
ON CONFLICT DO NOTHING;
