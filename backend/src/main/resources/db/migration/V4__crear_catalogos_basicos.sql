CREATE TABLE categoria (
    id_categoria BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(250),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',

    CONSTRAINT ck_categoria_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE UNIQUE INDEX uq_categoria_nombre_ci ON categoria (lower(nombre));
CREATE INDEX idx_categoria_estado ON categoria (estado);

CREATE TABLE marca (
    id_marca BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(120) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',

    CONSTRAINT ck_marca_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE UNIQUE INDEX uq_marca_nombre_ci ON marca (lower(nombre));
CREATE INDEX idx_marca_estado ON marca (estado);

CREATE TABLE unidad_medida (
    id_unidad_medida BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL,
    nombre VARCHAR(80) NOT NULL,
    permite_decimales BOOLEAN NOT NULL DEFAULT FALSE,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',

    CONSTRAINT ck_unidad_medida_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE UNIQUE INDEX uq_unidad_medida_codigo_ci ON unidad_medida (lower(codigo));
CREATE INDEX idx_unidad_medida_estado ON unidad_medida (estado);

INSERT INTO permiso (codigo, nombre, modulo, descripcion)
VALUES
    ('CAT_CATEGORIAS_VER', 'Consultar categorías', 'Catálogo', 'Listar y buscar categorías'),
    ('CAT_CATEGORIAS_CREAR', 'Crear categorías', 'Catálogo', 'Registrar categorías de productos'),
    ('CAT_CATEGORIAS_EDITAR', 'Editar categorías', 'Catálogo', 'Modificar categorías existentes'),
    ('CAT_CATEGORIAS_ESTADO', 'Cambiar estado de categorías', 'Catálogo', 'Activar o inactivar categorías'),
    ('CAT_MARCAS_VER', 'Consultar marcas', 'Catálogo', 'Listar y buscar marcas'),
    ('CAT_MARCAS_CREAR', 'Crear marcas', 'Catálogo', 'Registrar marcas de productos'),
    ('CAT_MARCAS_EDITAR', 'Editar marcas', 'Catálogo', 'Modificar marcas y su estado'),
    ('CAT_UNIDADES_VER', 'Consultar unidades de medida', 'Catálogo', 'Listar y buscar unidades de medida'),
    ('CAT_UNIDADES_CREAR', 'Crear unidades de medida', 'Catálogo', 'Registrar unidades de medida'),
    ('CAT_UNIDADES_EDITAR', 'Editar unidades de medida', 'Catálogo', 'Modificar unidades de medida y su estado')
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
      'CAT_CATEGORIAS_VER',
      'CAT_CATEGORIAS_CREAR',
      'CAT_CATEGORIAS_EDITAR',
      'CAT_CATEGORIAS_ESTADO',
      'CAT_MARCAS_VER',
      'CAT_MARCAS_CREAR',
      'CAT_MARCAS_EDITAR',
      'CAT_UNIDADES_VER',
      'CAT_UNIDADES_CREAR',
      'CAT_UNIDADES_EDITAR'
  )
ON CONFLICT DO NOTHING;
