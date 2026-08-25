DROP VIEW IF EXISTS vw_cuentas_cobrar_pendientes;

ALTER TABLE cuenta_cobrar
    ADD COLUMN id_cliente BIGINT,
    ADD COLUMN origen VARCHAR(30),
    ADD COLUMN fecha_origen DATE,
    ADD COLUMN documento_referencia VARCHAR(120),
    ADD COLUMN observacion VARCHAR(500),
    ADD COLUMN id_usuario_creacion BIGINT,
    ADD COLUMN fecha_registro TIMESTAMPTZ;

UPDATE cuenta_cobrar cc
SET id_cliente = v.id_cliente,
    origen = 'VENTA',
    fecha_origen = (v.fecha_hora AT TIME ZONE 'America/Lima')::DATE,
    id_usuario_creacion = v.id_vendedor,
    fecha_registro = v.fecha_hora
FROM venta v
WHERE v.id_venta = cc.id_venta;

ALTER TABLE cuenta_cobrar
    ALTER COLUMN id_venta DROP NOT NULL,
    ALTER COLUMN id_cliente SET NOT NULL,
    ALTER COLUMN origen SET NOT NULL,
    ALTER COLUMN fecha_origen SET NOT NULL,
    ALTER COLUMN id_usuario_creacion SET NOT NULL,
    ALTER COLUMN fecha_registro SET NOT NULL,
    ALTER COLUMN fecha_registro SET DEFAULT CURRENT_TIMESTAMP,
    ADD CONSTRAINT fk_cuenta_cobrar_cliente
        FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente),
    ADD CONSTRAINT fk_cuenta_cobrar_usuario_creacion
        FOREIGN KEY (id_usuario_creacion) REFERENCES usuario (id_usuario),
    ADD CONSTRAINT ck_cuenta_cobrar_origen CHECK (
        (origen = 'VENTA' AND id_venta IS NOT NULL)
        OR (origen = 'SALDO_INICIAL' AND id_venta IS NULL)
    );

CREATE INDEX idx_cuenta_cobrar_cliente_estado
    ON cuenta_cobrar (id_cliente, estado);
CREATE INDEX idx_cuenta_cobrar_origen_fecha
    ON cuenta_cobrar (origen, fecha_origen DESC);

ALTER TABLE pago_cliente
    ALTER COLUMN id_venta DROP NOT NULL;

CREATE VIEW vw_cuentas_cobrar_pendientes AS
SELECT
    cc.id_cuenta_cobrar,
    cc.id_venta,
    cc.id_cliente,
    cc.total,
    cc.importe_pagado,
    cc.saldo_pendiente,
    cc.fecha_vencimiento,
    CASE
        WHEN cc.saldo_pendiente > 0
         AND cc.fecha_vencimiento < CURRENT_DATE THEN 'VENCIDO'
        ELSE cc.estado
    END AS estado
FROM cuenta_cobrar cc
WHERE cc.saldo_pendiente > 0
  AND cc.estado <> 'ANULADO';

INSERT INTO permiso (codigo, nombre, modulo, descripcion)
VALUES (
    'CXC_SALDOS_CREAR',
    'Registrar saldos iniciales de clientes',
    'Cuentas por cobrar',
    'Crear cuentas por cobrar históricas sin generar venta, inventario, caja ni comprobante SUNAT'
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
  AND p.codigo = 'CXC_SALDOS_CREAR'
ON CONFLICT DO NOTHING;
