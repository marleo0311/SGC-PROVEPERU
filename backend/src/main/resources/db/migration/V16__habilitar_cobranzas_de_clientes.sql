INSERT INTO permiso (codigo, nombre, modulo, descripcion)
VALUES
    ('CXC_CUENTAS_VER', 'Consultar cuentas por cobrar', 'Cuentas por cobrar', 'Consultar saldos, vencimientos e historial de pagos de clientes'),
    ('CXC_CUENTAS_EDITAR', 'Editar vencimiento de cuentas por cobrar', 'Cuentas por cobrar', 'Configurar la fecha de vencimiento de una cuenta de cliente'),
    ('CXC_PAGOS_CREAR', 'Registrar pagos de clientes', 'Cuentas por cobrar', 'Registrar cobros parciales o totales conservando su historial')
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
      'CXC_CUENTAS_VER',
      'CXC_CUENTAS_EDITAR',
      'CXC_PAGOS_CREAR'
  )
ON CONFLICT DO NOTHING;
