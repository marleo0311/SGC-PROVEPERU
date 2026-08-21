INSERT INTO rol (nombre, descripcion, estado)
SELECT 'Administrador', 'Acceso administrativo general al sistema', 'ACTIVO'
WHERE NOT EXISTS (
    SELECT 1 FROM rol WHERE lower(nombre) = lower('Administrador')
);

INSERT INTO permiso (codigo, nombre, modulo, descripcion)
VALUES
    ('SEG_USUARIOS_VER', 'Consultar usuarios', 'Seguridad', 'Listar, buscar y consultar usuarios'),
    ('SEG_USUARIOS_CREAR', 'Crear usuarios', 'Seguridad', 'Registrar nuevas cuentas de usuario'),
    ('SEG_USUARIOS_EDITAR', 'Editar usuarios', 'Seguridad', 'Modificar datos permitidos de usuarios'),
    ('SEG_USUARIOS_ESTADO', 'Cambiar estado de usuarios', 'Seguridad', 'Activar o suspender cuentas de usuario'),
    ('SEG_USUARIOS_PASSWORD', 'Cambiar contraseñas', 'Seguridad', 'Restablecer contraseñas de usuarios'),
    ('SEG_ROLES_VER', 'Consultar roles', 'Seguridad', 'Listar roles y consultar sus permisos'),
    ('SEG_ROLES_CREAR', 'Crear roles', 'Seguridad', 'Registrar nuevos roles del sistema'),
    ('SEG_ROLES_PERMISOS', 'Asignar permisos a roles', 'Seguridad', 'Actualizar los permisos asociados a un rol'),
    ('SEG_PERMISOS_VER', 'Consultar permisos', 'Seguridad', 'Listar los permisos disponibles')
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
      'SEG_USUARIOS_VER',
      'SEG_USUARIOS_CREAR',
      'SEG_USUARIOS_EDITAR',
      'SEG_USUARIOS_ESTADO',
      'SEG_USUARIOS_PASSWORD',
      'SEG_ROLES_VER',
      'SEG_ROLES_CREAR',
      'SEG_ROLES_PERMISOS',
      'SEG_PERMISOS_VER'
  )
ON CONFLICT DO NOTHING;
