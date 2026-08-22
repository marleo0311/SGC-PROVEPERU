CREATE TABLE venta (
    id_venta BIGSERIAL PRIMARY KEY,
    id_cliente BIGINT,
    id_vendedor BIGINT NOT NULL,
    id_pedido BIGINT,
    id_sede BIGINT NOT NULL,
    fecha_hora TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    tipo_venta VARCHAR(20) NOT NULL,
    condicion_pago VARCHAR(20) NOT NULL,
    tipo_comprobante VARCHAR(30) NOT NULL DEFAULT 'NOTA_VENTA',
    subtotal NUMERIC(14,2) NOT NULL,
    igv NUMERIC(14,2) NOT NULL,
    descuento_total NUMERIC(14,2) NOT NULL DEFAULT 0,
    total NUMERIC(14,2) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'REGISTRADA',
    fecha_anulacion TIMESTAMPTZ,
    motivo_anulacion VARCHAR(300),

    CONSTRAINT fk_venta_cliente
        FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente),
    CONSTRAINT fk_venta_vendedor
        FOREIGN KEY (id_vendedor) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_venta_pedido
        FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido),
    CONSTRAINT fk_venta_sede
        FOREIGN KEY (id_sede) REFERENCES sede (id_sede),
    CONSTRAINT uq_venta_pedido UNIQUE (id_pedido),
    CONSTRAINT ck_venta_tipo CHECK (
        tipo_venta IN ('MINORISTA', 'MAYORISTA')
    ),
    CONSTRAINT ck_venta_condicion CHECK (
        condicion_pago IN ('CONTADO', 'CREDITO', 'PARCIAL')
    ),
    CONSTRAINT ck_venta_comprobante CHECK (
        tipo_comprobante IN ('NOTA_VENTA')
    ),
    CONSTRAINT ck_venta_cliente_credito CHECK (
        condicion_pago = 'CONTADO' OR id_cliente IS NOT NULL
    ),
    CONSTRAINT ck_venta_importes CHECK (
        subtotal >= 0
        AND igv >= 0
        AND descuento_total >= 0
        AND total = subtotal + igv
        AND total > 0
    ),
    CONSTRAINT ck_venta_estado CHECK (
        estado IN ('REGISTRADA', 'ANULADA', 'DEVUELTA_PARCIAL')
    ),
    CONSTRAINT ck_venta_anulacion CHECK (
        (estado = 'ANULADA'
            AND fecha_anulacion IS NOT NULL
            AND motivo_anulacion IS NOT NULL)
        OR (estado <> 'ANULADA'
            AND fecha_anulacion IS NULL
            AND motivo_anulacion IS NULL)
    )
);

CREATE INDEX idx_venta_fecha ON venta (fecha_hora DESC);
CREATE INDEX idx_venta_cliente_fecha
    ON venta (id_cliente, fecha_hora DESC);
CREATE INDEX idx_venta_vendedor_fecha
    ON venta (id_vendedor, fecha_hora DESC);
CREATE INDEX idx_venta_estado_fecha
    ON venta (estado, fecha_hora DESC);

CREATE TABLE detalle_venta (
    id_detalle_venta BIGSERIAL PRIMARY KEY,
    id_venta BIGINT NOT NULL,
    id_producto BIGINT NOT NULL,
    id_unidad_medida BIGINT NOT NULL,
    cantidad NUMERIC(14,3) NOT NULL,
    cantidad_base NUMERIC(14,3) NOT NULL,
    precio_unitario NUMERIC(14,2) NOT NULL,
    descuento NUMERIC(14,2) NOT NULL DEFAULT 0,
    subtotal NUMERIC(14,2) NOT NULL,

    CONSTRAINT fk_detalle_venta_venta
        FOREIGN KEY (id_venta) REFERENCES venta (id_venta),
    CONSTRAINT fk_detalle_venta_producto
        FOREIGN KEY (id_producto) REFERENCES producto (id_producto),
    CONSTRAINT fk_detalle_venta_unidad
        FOREIGN KEY (id_unidad_medida) REFERENCES unidad_medida (id_unidad_medida),
    CONSTRAINT uq_detalle_venta_producto
        UNIQUE (id_venta, id_producto),
    CONSTRAINT ck_detalle_venta_cantidad CHECK (
        cantidad > 0 AND cantidad_base > 0
    ),
    CONSTRAINT ck_detalle_venta_precio CHECK (precio_unitario > 0),
    CONSTRAINT ck_detalle_venta_descuento CHECK (
        descuento >= 0
        AND descuento <= round(cantidad * precio_unitario, 2)
    ),
    CONSTRAINT ck_detalle_venta_subtotal CHECK (
        subtotal = round(cantidad * precio_unitario, 2) - descuento
        AND subtotal >= 0
    )
);

CREATE INDEX idx_detalle_venta_venta
    ON detalle_venta (id_venta);
CREATE INDEX idx_detalle_venta_producto
    ON detalle_venta (id_producto);

CREATE TABLE cuenta_cobrar (
    id_cuenta_cobrar BIGSERIAL PRIMARY KEY,
    id_venta BIGINT NOT NULL,
    total NUMERIC(14,2) NOT NULL,
    importe_pagado NUMERIC(14,2) NOT NULL DEFAULT 0,
    saldo_pendiente NUMERIC(14,2) NOT NULL,
    fecha_vencimiento DATE,
    estado VARCHAR(30) NOT NULL,

    CONSTRAINT fk_cuenta_cobrar_venta
        FOREIGN KEY (id_venta) REFERENCES venta (id_venta),
    CONSTRAINT uq_cuenta_cobrar_venta UNIQUE (id_venta),
    CONSTRAINT ck_cuenta_cobrar_importes CHECK (
        total > 0
        AND importe_pagado >= 0
        AND importe_pagado <= total
        AND saldo_pendiente >= 0
        AND (
            (estado = 'ANULADO' AND saldo_pendiente = 0)
            OR (estado <> 'ANULADO'
                AND saldo_pendiente = total - importe_pagado)
        )
    ),
    CONSTRAINT ck_cuenta_cobrar_estado CHECK (
        estado IN ('PENDIENTE', 'PARCIAL', 'PAGADO', 'VENCIDO', 'ANULADO')
    )
);

CREATE INDEX idx_cuenta_cobrar_estado_vencimiento
    ON cuenta_cobrar (estado, fecha_vencimiento);

CREATE TABLE pago_cliente (
    id_pago_cliente BIGSERIAL PRIMARY KEY,
    id_venta BIGINT NOT NULL,
    id_cuenta_cobrar BIGINT,
    id_metodo_pago BIGINT NOT NULL,
    id_usuario BIGINT NOT NULL,
    monto NUMERIC(14,2) NOT NULL,
    referencia VARCHAR(120),
    fecha_hora TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_pago_cliente_venta
        FOREIGN KEY (id_venta) REFERENCES venta (id_venta),
    CONSTRAINT fk_pago_cliente_cuenta
        FOREIGN KEY (id_cuenta_cobrar) REFERENCES cuenta_cobrar (id_cuenta_cobrar),
    CONSTRAINT fk_pago_cliente_metodo
        FOREIGN KEY (id_metodo_pago) REFERENCES metodo_pago (id_metodo_pago),
    CONSTRAINT fk_pago_cliente_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT ck_pago_cliente_monto CHECK (monto > 0)
);

CREATE INDEX idx_pago_cliente_venta_fecha
    ON pago_cliente (id_venta, fecha_hora DESC);
CREATE INDEX idx_pago_cliente_cuenta_fecha
    ON pago_cliente (id_cuenta_cobrar, fecha_hora DESC);
CREATE INDEX idx_pago_cliente_metodo_fecha
    ON pago_cliente (id_metodo_pago, fecha_hora DESC);

CREATE VIEW vw_cuentas_cobrar_pendientes AS
SELECT
    cc.id_cuenta_cobrar,
    cc.id_venta,
    v.id_cliente,
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
JOIN venta v ON v.id_venta = cc.id_venta
WHERE cc.saldo_pendiente > 0
  AND cc.estado <> 'ANULADO';

INSERT INTO permiso (codigo, nombre, modulo, descripcion)
VALUES
    ('VEN_VENTAS_VER', 'Consultar ventas', 'Ventas', 'Listar ventas, consultar sus detalles, pagos y saldo'),
    ('VEN_VENTAS_CREAR', 'Registrar ventas', 'Ventas', 'Registrar y confirmar ventas directas o procedentes de pedidos'),
    ('VEN_VENTAS_ANULAR', 'Anular ventas', 'Ventas', 'Anular ventas y revertir sus salidas de inventario'),
    ('VEN_COMPROBANTES_VER', 'Consultar comprobantes de venta', 'Ventas', 'Consultar la nota de venta interna generada'),
    ('VEN_DESCUENTOS_APLICAR', 'Aplicar descuentos en ventas', 'Ventas', 'Autorizar descuentos monetarios en los detalles de venta')
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
      'VEN_VENTAS_VER',
      'VEN_VENTAS_CREAR',
      'VEN_VENTAS_ANULAR',
      'VEN_COMPROBANTES_VER',
      'VEN_DESCUENTOS_APLICAR'
  )
ON CONFLICT DO NOTHING;
