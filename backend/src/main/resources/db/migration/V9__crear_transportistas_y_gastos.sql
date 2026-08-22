CREATE TABLE transportista (
    id_transportista BIGSERIAL PRIMARY KEY,
    tipo_documento VARCHAR(20),
    numero_documento VARCHAR(20),
    nombre_razon_social VARCHAR(200) NOT NULL,
    empresa_transporte VARCHAR(180),
    telefono VARCHAR(30),
    direccion VARCHAR(250),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_transportista_documento UNIQUE (tipo_documento, numero_documento),
    CONSTRAINT ck_transportista_documento CHECK (
        (tipo_documento IS NULL AND numero_documento IS NULL)
        OR (tipo_documento = 'DNI' AND numero_documento ~ '^[0-9]{8}$')
        OR (tipo_documento = 'RUC' AND numero_documento ~ '^[0-9]{11}$')
    ),
    CONSTRAINT ck_transportista_nombre CHECK (btrim(nombre_razon_social) <> ''),
    CONSTRAINT ck_transportista_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE INDEX idx_transportista_nombre ON transportista (lower(nombre_razon_social));
CREATE INDEX idx_transportista_empresa ON transportista (lower(empresa_transporte));
CREATE INDEX idx_transportista_estado ON transportista (estado);

CREATE TABLE gasto (
    id_gasto BIGSERIAL PRIMARY KEY,
    id_compra BIGINT,
    id_transportista BIGINT,
    id_usuario BIGINT NOT NULL,
    tipo_gasto VARCHAR(50) NOT NULL,
    descripcion VARCHAR(250),
    importe NUMERIC(14,2) NOT NULL,
    fecha DATE NOT NULL,
    numero_comprobante VARCHAR(60),
    fecha_registro TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_gasto_transportista
        FOREIGN KEY (id_transportista) REFERENCES transportista (id_transportista),
    CONSTRAINT fk_gasto_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT ck_gasto_tipo CHECK (
        tipo_gasto IN ('TRANSPORTE', 'CARGA', 'DESCARGA', 'MOVILIDAD', 'OTRO')
    ),
    CONSTRAINT ck_gasto_importe CHECK (importe > 0),
    CONSTRAINT ck_gasto_transportista CHECK (
        tipo_gasto <> 'TRANSPORTE' OR id_transportista IS NOT NULL
    )
);

COMMENT ON COLUMN gasto.id_compra IS
    'Compra opcional; V10 añadirá la clave foránea cuando se cree la tabla compra';

CREATE INDEX idx_gasto_fecha ON gasto (fecha DESC);
CREATE INDEX idx_gasto_tipo_fecha ON gasto (tipo_gasto, fecha DESC);
CREATE INDEX idx_gasto_transportista_fecha ON gasto (id_transportista, fecha DESC);
CREATE INDEX idx_gasto_compra ON gasto (id_compra) WHERE id_compra IS NOT NULL;

INSERT INTO permiso (codigo, nombre, modulo, descripcion)
VALUES
    ('TRN_TRANSPORTISTAS_VER', 'Consultar transportistas', 'Transportistas', 'Listar, buscar y consultar transportistas'),
    ('TRN_TRANSPORTISTAS_CREAR', 'Crear transportistas', 'Transportistas', 'Registrar transportistas'),
    ('TRN_TRANSPORTISTAS_EDITAR', 'Editar transportistas', 'Transportistas', 'Modificar los datos de transportistas'),
    ('TRN_TRANSPORTISTAS_ESTADO', 'Cambiar estado de transportistas', 'Transportistas', 'Activar o inactivar transportistas'),
    ('TRN_GASTOS_VER', 'Consultar gastos', 'Transportistas', 'Consultar gastos de transporte y gastos relacionados'),
    ('TRN_GASTOS_CREAR', 'Crear gastos', 'Transportistas', 'Registrar gastos con usuario responsable')
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
      'TRN_TRANSPORTISTAS_VER',
      'TRN_TRANSPORTISTAS_CREAR',
      'TRN_TRANSPORTISTAS_EDITAR',
      'TRN_TRANSPORTISTAS_ESTADO',
      'TRN_GASTOS_VER',
      'TRN_GASTOS_CREAR'
  )
ON CONFLICT DO NOTHING;
