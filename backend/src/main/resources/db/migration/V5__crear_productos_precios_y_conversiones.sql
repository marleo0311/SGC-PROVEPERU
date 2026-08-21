CREATE TABLE producto (
    id_producto BIGSERIAL PRIMARY KEY,
    id_categoria BIGINT NOT NULL,
    id_marca BIGINT,
    id_unidad_base BIGINT NOT NULL,
    codigo_interno VARCHAR(60) NOT NULL,
    codigo_barras VARCHAR(80),
    nombre VARCHAR(180) NOT NULL,
    descripcion VARCHAR(300),
    stock_minimo NUMERIC(14,3) NOT NULL DEFAULT 0,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_producto_categoria FOREIGN KEY (id_categoria) REFERENCES categoria (id_categoria),
    CONSTRAINT fk_producto_marca FOREIGN KEY (id_marca) REFERENCES marca (id_marca),
    CONSTRAINT fk_producto_unidad_base FOREIGN KEY (id_unidad_base) REFERENCES unidad_medida (id_unidad_medida),
    CONSTRAINT ck_producto_stock_minimo CHECK (stock_minimo >= 0),
    CONSTRAINT ck_producto_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE UNIQUE INDEX uq_producto_codigo_interno_ci ON producto (lower(codigo_interno));
CREATE UNIQUE INDEX uq_producto_codigo_barras_ci
    ON producto (lower(codigo_barras))
    WHERE codigo_barras IS NOT NULL;
CREATE INDEX idx_producto_categoria ON producto (id_categoria);
CREATE INDEX idx_producto_marca ON producto (id_marca);
CREATE INDEX idx_producto_unidad_base ON producto (id_unidad_base);
CREATE INDEX idx_producto_estado ON producto (estado);
CREATE INDEX idx_producto_nombre_ci ON producto (lower(nombre));

CREATE TABLE producto_unidad_conversion (
    id_conversion BIGSERIAL PRIMARY KEY,
    id_producto BIGINT NOT NULL,
    id_unidad_origen BIGINT NOT NULL,
    id_unidad_destino BIGINT NOT NULL,
    factor_conversion NUMERIC(18,6) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',

    CONSTRAINT fk_conversion_producto FOREIGN KEY (id_producto) REFERENCES producto (id_producto),
    CONSTRAINT fk_conversion_unidad_origen FOREIGN KEY (id_unidad_origen) REFERENCES unidad_medida (id_unidad_medida),
    CONSTRAINT fk_conversion_unidad_destino FOREIGN KEY (id_unidad_destino) REFERENCES unidad_medida (id_unidad_medida),
    CONSTRAINT uq_conversion_producto_unidades UNIQUE (id_producto, id_unidad_origen, id_unidad_destino),
    CONSTRAINT ck_conversion_unidades_distintas CHECK (id_unidad_origen <> id_unidad_destino),
    CONSTRAINT ck_conversion_factor_positivo CHECK (factor_conversion > 0),
    CONSTRAINT ck_conversion_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE INDEX idx_conversion_producto ON producto_unidad_conversion (id_producto);

CREATE TABLE precio_producto (
    id_precio BIGSERIAL PRIMARY KEY,
    id_producto BIGINT NOT NULL,
    tipo_precio VARCHAR(30) NOT NULL,
    monto NUMERIC(14,2) NOT NULL,
    vigente_desde DATE NOT NULL,
    vigente_hasta DATE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',

    CONSTRAINT fk_precio_producto FOREIGN KEY (id_producto) REFERENCES producto (id_producto),
    CONSTRAINT ck_precio_monto_positivo CHECK (monto > 0),
    CONSTRAINT ck_precio_vigencia CHECK (vigente_hasta IS NULL OR vigente_hasta >= vigente_desde),
    CONSTRAINT ck_precio_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE INDEX idx_precio_producto_tipo_vigencia
    ON precio_producto (id_producto, tipo_precio, vigente_desde DESC);

INSERT INTO permiso (codigo, nombre, modulo, descripcion)
VALUES
    ('CAT_PRODUCTOS_VER', 'Consultar productos', 'Catálogo', 'Listar, buscar y consultar productos'),
    ('CAT_PRODUCTOS_CREAR', 'Crear productos', 'Catálogo', 'Registrar productos y sus precios iniciales'),
    ('CAT_PRODUCTOS_EDITAR', 'Editar productos', 'Catálogo', 'Modificar datos permitidos de productos'),
    ('CAT_PRODUCTOS_ESTADO', 'Cambiar estado de productos', 'Catálogo', 'Activar o inactivar productos'),
    ('CAT_CONVERSIONES_VER', 'Consultar conversiones', 'Catálogo', 'Listar conversiones de unidades por producto'),
    ('CAT_CONVERSIONES_CREAR', 'Crear conversiones', 'Catálogo', 'Registrar conversiones para productos fraccionables'),
    ('CAT_PRECIOS_VER', 'Consultar precios', 'Catálogo', 'Consultar el historial de precios por producto'),
    ('CAT_PRECIOS_CREAR', 'Crear precios', 'Catálogo', 'Registrar nuevas vigencias de precios')
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
      'CAT_PRODUCTOS_VER',
      'CAT_PRODUCTOS_CREAR',
      'CAT_PRODUCTOS_EDITAR',
      'CAT_PRODUCTOS_ESTADO',
      'CAT_CONVERSIONES_VER',
      'CAT_CONVERSIONES_CREAR',
      'CAT_PRECIOS_VER',
      'CAT_PRECIOS_CREAR'
  )
ON CONFLICT DO NOTHING;
