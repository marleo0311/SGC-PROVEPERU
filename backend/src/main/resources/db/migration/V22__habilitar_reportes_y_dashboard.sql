INSERT INTO permiso (codigo, nombre, modulo, descripcion)
VALUES (
    'REP_REPORTES_VER',
    'Consultar reportes y dashboard',
    'Reportes',
    'Consultar indicadores de ventas, inventario, finanzas y caja'
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
  AND p.codigo = 'REP_REPORTES_VER'
ON CONFLICT DO NOTHING;
