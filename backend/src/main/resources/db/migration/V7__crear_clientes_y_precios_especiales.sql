CREATE TABLE cliente (
    id_cliente BIGSERIAL PRIMARY KEY,
    tipo_persona VARCHAR(20) NOT NULL,
    tipo_documento VARCHAR(20) NOT NULL,
    numero_documento VARCHAR(20) NOT NULL,
    nombres VARCHAR(120),
    apellidos VARCHAR(120),
    razon_social VARCHAR(200),
    nombre_comercial VARCHAR(180),
    direccion VARCHAR(250),
    telefono VARCHAR(30),
    whatsapp VARCHAR(30),
    correo VARCHAR(180),
    permite_credito BOOLEAN NOT NULL DEFAULT FALSE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_cliente_numero_documento UNIQUE (numero_documento),
    CONSTRAINT ck_cliente_tipo_persona CHECK (tipo_persona IN ('NATURAL', 'JURIDICA')),
    CONSTRAINT ck_cliente_tipo_documento CHECK (tipo_documento IN ('DNI', 'RUC')),
    CONSTRAINT ck_cliente_estado CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT ck_cliente_datos_persona CHECK (
        (
            tipo_persona = 'NATURAL'
            AND tipo_documento = 'DNI'
            AND numero_documento ~ '^[0-9]{8}$'
            AND nombres IS NOT NULL
            AND btrim(nombres) <> ''
            AND apellidos IS NOT NULL
            AND btrim(apellidos) <> ''
            AND razon_social IS NULL
        )
        OR
        (
            tipo_persona = 'JURIDICA'
            AND tipo_documento = 'RUC'
            AND numero_documento ~ '^[0-9]{11}$'
            AND razon_social IS NOT NULL
            AND btrim(razon_social) <> ''
            AND nombres IS NULL
            AND apellidos IS NULL
        )
    )
);

CREATE INDEX idx_cliente_nombre_natural
    ON cliente (lower(nombres), lower(apellidos))
    WHERE tipo_persona = 'NATURAL';
CREATE INDEX idx_cliente_razon_social
    ON cliente (lower(razon_social))
    WHERE tipo_persona = 'JURIDICA';
CREATE INDEX idx_cliente_estado ON cliente (estado);
CREATE INDEX idx_cliente_permite_credito ON cliente (permite_credito);

CREATE TABLE cliente_precio_especial (
    id_cliente_precio BIGSERIAL PRIMARY KEY,
    id_cliente BIGINT NOT NULL,
    id_producto BIGINT NOT NULL,
    precio NUMERIC(14,2) NOT NULL,
    vigente_desde DATE NOT NULL,
    vigente_hasta DATE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_cliente_precio_cliente FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente),
    CONSTRAINT fk_cliente_precio_producto FOREIGN KEY (id_producto) REFERENCES producto (id_producto),
    CONSTRAINT ck_cliente_precio_positivo CHECK (precio > 0),
    CONSTRAINT ck_cliente_precio_vigencia CHECK (
        vigente_hasta IS NULL OR vigente_hasta >= vigente_desde
    ),
    CONSTRAINT ck_cliente_precio_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE INDEX idx_cliente_precio_cliente_producto
    ON cliente_precio_especial (id_cliente, id_producto, vigente_desde DESC);

INSERT INTO permiso (codigo, nombre, modulo, descripcion)
VALUES
    ('CLI_CLIENTES_VER', 'Consultar clientes', 'Clientes', 'Listar, buscar y consultar clientes'),
    ('CLI_CLIENTES_CREAR', 'Crear clientes', 'Clientes', 'Registrar personas naturales y jurídicas'),
    ('CLI_CLIENTES_EDITAR', 'Editar clientes', 'Clientes', 'Modificar los datos permitidos de clientes'),
    ('CLI_CLIENTES_ESTADO', 'Cambiar estado de clientes', 'Clientes', 'Activar o inactivar clientes'),
    ('CLI_HISTORIAL_VER', 'Consultar historial de clientes', 'Clientes', 'Consultar el historial comercial del cliente'),
    ('CLI_PRECIOS_VER', 'Consultar precios especiales', 'Clientes', 'Consultar precios especiales por cliente'),
    ('CLI_PRECIOS_CREAR', 'Crear precios especiales', 'Clientes', 'Registrar precios autorizados por cliente y producto')
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
      'CLI_CLIENTES_VER',
      'CLI_CLIENTES_CREAR',
      'CLI_CLIENTES_EDITAR',
      'CLI_CLIENTES_ESTADO',
      'CLI_HISTORIAL_VER',
      'CLI_PRECIOS_VER',
      'CLI_PRECIOS_CREAR'
  )
ON CONFLICT DO NOTHING;
