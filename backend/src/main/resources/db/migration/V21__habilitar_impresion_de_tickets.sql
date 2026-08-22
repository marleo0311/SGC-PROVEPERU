INSERT INTO permiso (codigo, nombre, modulo, descripcion)
VALUES (
    'VEN_TICKETS_IMPRIMIR',
    'Generar tickets de venta',
    'Ticketera',
    'Generar la representación térmica de 58 mm u 80 mm de un comprobante'
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
  AND p.codigo = 'VEN_TICKETS_IMPRIMIR'
ON CONFLICT DO NOTHING;
