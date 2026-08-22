INSERT INTO metodo_pago (codigo, nombre, estado)
VALUES
    ('EFECTIVO', 'Efectivo', 'ACTIVO'),
    ('TRANSFERENCIA', 'Transferencia bancaria', 'ACTIVO'),
    ('YAPE', 'Yape', 'ACTIVO'),
    ('PLIN', 'Plin', 'ACTIVO'),
    ('TARJETA', 'Tarjeta', 'ACTIVO')
ON CONFLICT (codigo) DO UPDATE SET
    nombre = EXCLUDED.nombre;

CREATE TABLE cuenta_pagar (
    id_cuenta_pagar BIGSERIAL PRIMARY KEY,
    id_compra BIGINT NOT NULL,
    total NUMERIC(14,2) NOT NULL,
    importe_pagado NUMERIC(14,2) NOT NULL DEFAULT 0,
    saldo_pendiente NUMERIC(14,2) NOT NULL,
    fecha_vencimiento DATE,
    estado VARCHAR(30) NOT NULL,

    CONSTRAINT fk_cuenta_pagar_compra
        FOREIGN KEY (id_compra) REFERENCES compra (id_compra),
    CONSTRAINT uq_cuenta_pagar_compra UNIQUE (id_compra),
    CONSTRAINT ck_cuenta_pagar_importes CHECK (
        total > 0
        AND importe_pagado >= 0
        AND importe_pagado <= total
        AND saldo_pendiente >= 0
        AND saldo_pendiente = total - importe_pagado
    ),
    CONSTRAINT ck_cuenta_pagar_estado CHECK (
        estado IN ('PENDIENTE', 'PARCIAL', 'PAGADO', 'VENCIDO', 'ANULADO')
    )
);

CREATE INDEX idx_cuenta_pagar_estado_vencimiento
    ON cuenta_pagar (estado, fecha_vencimiento);

CREATE TABLE pago_proveedor (
    id_pago_proveedor BIGSERIAL PRIMARY KEY,
    id_cuenta_pagar BIGINT NOT NULL,
    id_metodo_pago BIGINT NOT NULL,
    id_usuario BIGINT NOT NULL,
    monto NUMERIC(14,2) NOT NULL,
    referencia VARCHAR(120),
    fecha_hora TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_pago_proveedor_cuenta
        FOREIGN KEY (id_cuenta_pagar) REFERENCES cuenta_pagar (id_cuenta_pagar),
    CONSTRAINT fk_pago_proveedor_metodo
        FOREIGN KEY (id_metodo_pago) REFERENCES metodo_pago (id_metodo_pago),
    CONSTRAINT fk_pago_proveedor_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT ck_pago_proveedor_monto CHECK (monto > 0)
);

CREATE INDEX idx_pago_proveedor_cuenta_fecha
    ON pago_proveedor (id_cuenta_pagar, fecha_hora DESC);
CREATE INDEX idx_pago_proveedor_metodo_fecha
    ON pago_proveedor (id_metodo_pago, fecha_hora DESC);

INSERT INTO cuenta_pagar (
    id_compra,
    total,
    importe_pagado,
    saldo_pendiente,
    fecha_vencimiento,
    estado
)
SELECT
    c.id_compra,
    c.total,
    0,
    c.total,
    NULL,
    'PENDIENTE'
FROM compra c
WHERE c.estado = 'RECIBIDA'
  AND c.condicion_pago IN ('CREDITO', 'PARCIAL')
ON CONFLICT (id_compra) DO NOTHING;

CREATE VIEW vw_cuentas_pagar_pendientes AS
SELECT
    cp.id_cuenta_pagar,
    cp.id_compra,
    c.id_proveedor,
    cp.total,
    cp.importe_pagado,
    cp.saldo_pendiente,
    cp.fecha_vencimiento,
    CASE
        WHEN cp.saldo_pendiente > 0
         AND cp.fecha_vencimiento < CURRENT_DATE THEN 'VENCIDO'
        ELSE cp.estado
    END AS estado
FROM cuenta_pagar cp
JOIN compra c ON c.id_compra = cp.id_compra
WHERE cp.saldo_pendiente > 0
  AND cp.estado <> 'ANULADO';

INSERT INTO permiso (codigo, nombre, modulo, descripcion)
VALUES
    ('CXP_CUENTAS_VER', 'Consultar cuentas por pagar', 'Cuentas por pagar', 'Consultar saldos, vencimientos e historial de pagos a proveedores'),
    ('CXP_CUENTAS_EDITAR', 'Editar vencimiento de cuentas por pagar', 'Cuentas por pagar', 'Configurar la fecha de vencimiento de una obligación'),
    ('CXP_PAGOS_CREAR', 'Registrar pagos a proveedores', 'Cuentas por pagar', 'Registrar pagos parciales o totales conservando su historial')
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
      'CXP_CUENTAS_VER',
      'CXP_CUENTAS_EDITAR',
      'CXP_PAGOS_CREAR'
  )
ON CONFLICT DO NOTHING;
