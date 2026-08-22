CREATE TABLE pedido (
    id_pedido BIGSERIAL PRIMARY KEY,
    id_cliente BIGINT,
    id_cotizacion BIGINT,
    id_usuario BIGINT NOT NULL,
    id_sede BIGINT NOT NULL,
    canal VARCHAR(20) NOT NULL,
    fecha_hora TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    estado VARCHAR(30) NOT NULL DEFAULT 'RECIBIDO',
    observacion VARCHAR(300),
    subtotal NUMERIC(14,2) NOT NULL,
    igv NUMERIC(14,2) NOT NULL,
    total NUMERIC(14,2) NOT NULL,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_pedido_cliente
        FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente),
    CONSTRAINT fk_pedido_cotizacion
        FOREIGN KEY (id_cotizacion) REFERENCES cotizacion (id_cotizacion),
    CONSTRAINT fk_pedido_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_pedido_sede
        FOREIGN KEY (id_sede) REFERENCES sede (id_sede),
    CONSTRAINT uq_pedido_cotizacion UNIQUE (id_cotizacion),
    CONSTRAINT ck_pedido_canal CHECK (
        canal IN ('PRESENCIAL', 'WHATSAPP')
    ),
    CONSTRAINT ck_pedido_estado CHECK (
        estado IN (
            'RECIBIDO',
            'COTIZADO',
            'CONFIRMADO',
            'PAGADO',
            'EN_PREPARACION',
            'LISTO',
            'ENTREGADO',
            'CANCELADO'
        )
    ),
    CONSTRAINT ck_pedido_importes CHECK (
        subtotal >= 0
        AND igv >= 0
        AND total = subtotal + igv
    )
);

CREATE INDEX idx_pedido_fecha ON pedido (fecha_hora DESC);
CREATE INDEX idx_pedido_cliente_fecha
    ON pedido (id_cliente, fecha_hora DESC);
CREATE INDEX idx_pedido_estado_fecha
    ON pedido (estado, fecha_hora DESC);

CREATE TABLE detalle_pedido (
    id_detalle_pedido BIGSERIAL PRIMARY KEY,
    id_pedido BIGINT NOT NULL,
    id_producto BIGINT NOT NULL,
    id_unidad_medida BIGINT NOT NULL,
    cantidad NUMERIC(14,3) NOT NULL,
    cantidad_base NUMERIC(14,3) NOT NULL,
    precio_unitario NUMERIC(14,2) NOT NULL,
    descuento NUMERIC(14,2) NOT NULL DEFAULT 0,
    subtotal NUMERIC(14,2) NOT NULL,

    CONSTRAINT fk_detalle_pedido_pedido
        FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido),
    CONSTRAINT fk_detalle_pedido_producto
        FOREIGN KEY (id_producto) REFERENCES producto (id_producto),
    CONSTRAINT fk_detalle_pedido_unidad
        FOREIGN KEY (id_unidad_medida) REFERENCES unidad_medida (id_unidad_medida),
    CONSTRAINT uq_detalle_pedido_producto
        UNIQUE (id_pedido, id_producto),
    CONSTRAINT ck_detalle_pedido_cantidad CHECK (
        cantidad > 0 AND cantidad_base > 0
    ),
    CONSTRAINT ck_detalle_pedido_precio CHECK (precio_unitario > 0),
    CONSTRAINT ck_detalle_pedido_descuento CHECK (
        descuento >= 0
        AND descuento <= round(cantidad * precio_unitario, 2)
    ),
    CONSTRAINT ck_detalle_pedido_subtotal CHECK (
        subtotal = round(cantidad * precio_unitario, 2) - descuento
        AND subtotal >= 0
    )
);

CREATE INDEX idx_detalle_pedido_pedido
    ON detalle_pedido (id_pedido);
CREATE INDEX idx_detalle_pedido_producto
    ON detalle_pedido (id_producto);

CREATE TABLE reserva_stock (
    id_reserva BIGSERIAL PRIMARY KEY,
    id_pedido BIGINT NOT NULL,
    id_detalle_pedido BIGINT NOT NULL,
    id_sede BIGINT NOT NULL,
    id_producto BIGINT NOT NULL,
    cantidad NUMERIC(14,3) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
    fecha_reserva TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_liberacion TIMESTAMPTZ,

    CONSTRAINT fk_reserva_pedido
        FOREIGN KEY (id_pedido) REFERENCES pedido (id_pedido),
    CONSTRAINT fk_reserva_detalle_pedido
        FOREIGN KEY (id_detalle_pedido) REFERENCES detalle_pedido (id_detalle_pedido),
    CONSTRAINT fk_reserva_sede
        FOREIGN KEY (id_sede) REFERENCES sede (id_sede),
    CONSTRAINT fk_reserva_producto
        FOREIGN KEY (id_producto) REFERENCES producto (id_producto),
    CONSTRAINT uq_reserva_detalle UNIQUE (id_detalle_pedido),
    CONSTRAINT ck_reserva_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_reserva_estado CHECK (
        estado IN ('ACTIVA', 'LIBERADA', 'CONSUMIDA')
    ),
    CONSTRAINT ck_reserva_liberacion CHECK (
        (estado = 'ACTIVA' AND fecha_liberacion IS NULL)
        OR (estado <> 'ACTIVA' AND fecha_liberacion IS NOT NULL)
    )
);

CREATE INDEX idx_reserva_pedido_estado
    ON reserva_stock (id_pedido, estado);
CREATE INDEX idx_reserva_sede_producto_estado
    ON reserva_stock (id_sede, id_producto, estado);

INSERT INTO permiso (codigo, nombre, modulo, descripcion)
VALUES
    ('PED_PEDIDOS_VER', 'Consultar pedidos', 'Pedidos', 'Listar y consultar pedidos y sus detalles'),
    ('PED_PEDIDOS_CREAR', 'Crear pedidos', 'Pedidos', 'Registrar pedidos presenciales o recibidos por WhatsApp'),
    ('PED_PEDIDOS_CONVERTIR', 'Convertir cotizaciones en pedidos', 'Pedidos', 'Crear un pedido a partir de una cotización aceptada'),
    ('PED_PEDIDOS_CONFIRMAR', 'Confirmar pedidos', 'Pedidos', 'Confirmar pedidos y reservar sus existencias'),
    ('PED_PEDIDOS_ESTADO', 'Gestionar estados de pedidos', 'Pedidos', 'Gestionar preparación, recojo y entrega de pedidos'),
    ('PED_PEDIDOS_CANCELAR', 'Cancelar pedidos', 'Pedidos', 'Cancelar pedidos y liberar sus reservas activas'),
    ('PED_RESERVAS_VER', 'Consultar reservas de pedidos', 'Pedidos', 'Consultar las existencias reservadas por un pedido')
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
      'PED_PEDIDOS_VER',
      'PED_PEDIDOS_CREAR',
      'PED_PEDIDOS_CONVERTIR',
      'PED_PEDIDOS_CONFIRMAR',
      'PED_PEDIDOS_ESTADO',
      'PED_PEDIDOS_CANCELAR',
      'PED_RESERVAS_VER'
  )
ON CONFLICT DO NOTHING;
