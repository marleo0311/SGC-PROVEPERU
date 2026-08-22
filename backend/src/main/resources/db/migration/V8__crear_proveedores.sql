CREATE TABLE proveedor (
    id_proveedor BIGSERIAL PRIMARY KEY,
    ruc VARCHAR(11) NOT NULL,
    razon_social VARCHAR(200) NOT NULL,
    nombre_comercial VARCHAR(180),
    direccion VARCHAR(250),
    telefono VARCHAR(30),
    correo VARCHAR(180),
    persona_contacto VARCHAR(180),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    fecha_registro TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_proveedor_ruc UNIQUE (ruc),
    CONSTRAINT ck_proveedor_ruc CHECK (ruc ~ '^[0-9]{11}$'),
    CONSTRAINT ck_proveedor_razon_social CHECK (btrim(razon_social) <> ''),
    CONSTRAINT ck_proveedor_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE INDEX idx_proveedor_razon_social ON proveedor (lower(razon_social));
CREATE INDEX idx_proveedor_nombre_comercial ON proveedor (lower(nombre_comercial));
CREATE INDEX idx_proveedor_estado ON proveedor (estado);

INSERT INTO permiso (codigo, nombre, modulo, descripcion)
VALUES
    ('PRV_PROVEEDORES_VER', 'Consultar proveedores', 'Proveedores', 'Listar, buscar y consultar proveedores'),
    ('PRV_PROVEEDORES_CREAR', 'Crear proveedores', 'Proveedores', 'Registrar proveedores'),
    ('PRV_PROVEEDORES_EDITAR', 'Editar proveedores', 'Proveedores', 'Modificar los datos de proveedores'),
    ('PRV_PROVEEDORES_ESTADO', 'Cambiar estado de proveedores', 'Proveedores', 'Activar o inactivar proveedores'),
    ('PRV_HISTORIAL_VER', 'Consultar historial de proveedor', 'Proveedores', 'Consultar las compras realizadas al proveedor')
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
      'PRV_PROVEEDORES_VER',
      'PRV_PROVEEDORES_CREAR',
      'PRV_PROVEEDORES_EDITAR',
      'PRV_PROVEEDORES_ESTADO',
      'PRV_HISTORIAL_VER'
  )
ON CONFLICT DO NOTHING;
