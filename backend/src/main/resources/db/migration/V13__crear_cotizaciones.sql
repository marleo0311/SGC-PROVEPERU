CREATE TABLE cotizacion (
    id_cotizacion BIGSERIAL PRIMARY KEY,
    id_cliente BIGINT,
    id_usuario BIGINT NOT NULL,
    fecha DATE NOT NULL,
    fecha_vencimiento DATE,
    subtotal NUMERIC(14,2) NOT NULL,
    igv NUMERIC(14,2) NOT NULL,
    total NUMERIC(14,2) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'PENDIENTE',
    fecha_registro TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_cotizacion_cliente
        FOREIGN KEY (id_cliente) REFERENCES cliente (id_cliente),
    CONSTRAINT fk_cotizacion_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT ck_cotizacion_vigencia CHECK (
        fecha_vencimiento IS NULL OR fecha_vencimiento >= fecha
    ),
    CONSTRAINT ck_cotizacion_importes CHECK (
        subtotal >= 0
        AND igv >= 0
        AND total = subtotal + igv
    ),
    CONSTRAINT ck_cotizacion_estado CHECK (
        estado IN ('PENDIENTE', 'ACEPTADA', 'RECHAZADA', 'VENCIDA', 'CONVERTIDA')
    )
);

CREATE INDEX idx_cotizacion_fecha ON cotizacion (fecha DESC);
CREATE INDEX idx_cotizacion_cliente_fecha
    ON cotizacion (id_cliente, fecha DESC);
CREATE INDEX idx_cotizacion_estado_vencimiento
    ON cotizacion (estado, fecha_vencimiento);

CREATE TABLE detalle_cotizacion (
    id_detalle_cotizacion BIGSERIAL PRIMARY KEY,
    id_cotizacion BIGINT NOT NULL,
    id_producto BIGINT NOT NULL,
    id_unidad_medida BIGINT NOT NULL,
    cantidad NUMERIC(14,3) NOT NULL,
    precio_unitario NUMERIC(14,2) NOT NULL,
    descuento NUMERIC(14,2) NOT NULL DEFAULT 0,
    subtotal NUMERIC(14,2) NOT NULL,

    CONSTRAINT fk_detalle_cotizacion_cotizacion
        FOREIGN KEY (id_cotizacion) REFERENCES cotizacion (id_cotizacion),
    CONSTRAINT fk_detalle_cotizacion_producto
        FOREIGN KEY (id_producto) REFERENCES producto (id_producto),
    CONSTRAINT fk_detalle_cotizacion_unidad
        FOREIGN KEY (id_unidad_medida) REFERENCES unidad_medida (id_unidad_medida),
    CONSTRAINT uq_detalle_cotizacion_producto
        UNIQUE (id_cotizacion, id_producto),
    CONSTRAINT ck_detalle_cotizacion_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_detalle_cotizacion_precio CHECK (precio_unitario > 0),
    CONSTRAINT ck_detalle_cotizacion_descuento CHECK (
        descuento >= 0
        AND descuento <= round(cantidad * precio_unitario, 2)
    ),
    CONSTRAINT ck_detalle_cotizacion_subtotal CHECK (
        subtotal = round(cantidad * precio_unitario, 2) - descuento
        AND subtotal >= 0
    )
);

CREATE INDEX idx_detalle_cotizacion_cotizacion
    ON detalle_cotizacion (id_cotizacion);
CREATE INDEX idx_detalle_cotizacion_producto
    ON detalle_cotizacion (id_producto);

INSERT INTO permiso (codigo, nombre, modulo, descripcion)
VALUES
    ('COT_COTIZACIONES_VER', 'Consultar cotizaciones', 'Cotizaciones', 'Listar, filtrar y consultar cotizaciones con disponibilidad'),
    ('COT_COTIZACIONES_CREAR', 'Crear cotizaciones', 'Cotizaciones', 'Registrar cotizaciones y calcular sus importes'),
    ('COT_COTIZACIONES_EDITAR', 'Editar cotizaciones', 'Cotizaciones', 'Modificar cotizaciones pendientes y vigentes'),
    ('COT_COTIZACIONES_ESTADO', 'Cambiar estado de cotizaciones', 'Cotizaciones', 'Aceptar o rechazar cotizaciones pendientes'),
    ('COT_DESCUENTOS_APLICAR', 'Aplicar descuentos en cotizaciones', 'Cotizaciones', 'Autorizar descuentos en los productos cotizados')
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
      'COT_COTIZACIONES_VER',
      'COT_COTIZACIONES_CREAR',
      'COT_COTIZACIONES_EDITAR',
      'COT_COTIZACIONES_ESTADO',
      'COT_DESCUENTOS_APLICAR'
  )
ON CONFLICT DO NOTHING;
