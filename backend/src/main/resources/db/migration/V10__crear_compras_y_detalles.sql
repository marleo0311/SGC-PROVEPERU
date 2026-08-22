CREATE TABLE compra (
    id_compra BIGSERIAL PRIMARY KEY,
    id_proveedor BIGINT NOT NULL,
    id_usuario BIGINT NOT NULL,
    fecha DATE NOT NULL,
    tipo_comprobante VARCHAR(30),
    numero_comprobante VARCHAR(60),
    condicion_pago VARCHAR(20) NOT NULL,
    subtotal NUMERIC(14,2) NOT NULL,
    igv NUMERIC(14,2) NOT NULL,
    gastos_adicionales NUMERIC(14,2) NOT NULL DEFAULT 0,
    total NUMERIC(14,2) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'REGISTRADA',
    fecha_registro TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_compra_proveedor
        FOREIGN KEY (id_proveedor) REFERENCES proveedor (id_proveedor),
    CONSTRAINT fk_compra_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT ck_compra_condicion_pago CHECK (
        condicion_pago IN ('CONTADO', 'CREDITO', 'PARCIAL')
    ),
    CONSTRAINT ck_compra_importes CHECK (
        subtotal >= 0
        AND igv >= 0
        AND gastos_adicionales >= 0
        AND total = subtotal + igv + gastos_adicionales
    ),
    CONSTRAINT ck_compra_estado CHECK (
        estado IN ('REGISTRADA', 'PARCIALMENTE_RECIBIDA', 'RECIBIDA', 'ANULADA')
    ),
    CONSTRAINT ck_compra_comprobante CHECK (
        (tipo_comprobante IS NULL AND numero_comprobante IS NULL)
        OR (tipo_comprobante IS NOT NULL AND btrim(tipo_comprobante) <> ''
            AND numero_comprobante IS NOT NULL AND btrim(numero_comprobante) <> '')
    )
);

CREATE INDEX idx_compra_fecha ON compra (fecha DESC);
CREATE INDEX idx_compra_proveedor_fecha ON compra (id_proveedor, fecha DESC);
CREATE INDEX idx_compra_estado_fecha ON compra (estado, fecha DESC);
CREATE UNIQUE INDEX uq_compra_proveedor_comprobante_ci
    ON compra (id_proveedor, lower(tipo_comprobante), lower(numero_comprobante))
    WHERE tipo_comprobante IS NOT NULL AND numero_comprobante IS NOT NULL;

CREATE TABLE detalle_compra (
    id_detalle_compra BIGSERIAL PRIMARY KEY,
    id_compra BIGINT NOT NULL,
    id_producto BIGINT NOT NULL,
    id_unidad_medida BIGINT NOT NULL,
    cantidad NUMERIC(14,3) NOT NULL,
    precio_compra NUMERIC(14,2) NOT NULL,
    subtotal NUMERIC(14,2) NOT NULL,

    CONSTRAINT fk_detalle_compra_compra
        FOREIGN KEY (id_compra) REFERENCES compra (id_compra),
    CONSTRAINT fk_detalle_compra_producto
        FOREIGN KEY (id_producto) REFERENCES producto (id_producto),
    CONSTRAINT fk_detalle_compra_unidad
        FOREIGN KEY (id_unidad_medida) REFERENCES unidad_medida (id_unidad_medida),
    CONSTRAINT uq_detalle_compra_producto_unidad
        UNIQUE (id_compra, id_producto, id_unidad_medida),
    CONSTRAINT ck_detalle_compra_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_detalle_compra_precio CHECK (precio_compra > 0),
    CONSTRAINT ck_detalle_compra_subtotal CHECK (subtotal > 0)
);

CREATE INDEX idx_detalle_compra_compra ON detalle_compra (id_compra);
CREATE INDEX idx_detalle_compra_producto ON detalle_compra (id_producto);

ALTER TABLE gasto
    ADD CONSTRAINT fk_gasto_compra
    FOREIGN KEY (id_compra) REFERENCES compra (id_compra);

COMMENT ON COLUMN gasto.id_compra IS 'Compra opcional relacionada con el gasto';

INSERT INTO permiso (codigo, nombre, modulo, descripcion)
VALUES
    ('CMP_COMPRAS_VER', 'Consultar compras', 'Compras', 'Listar, filtrar y consultar compras con sus detalles'),
    ('CMP_COMPRAS_CREAR', 'Crear compras', 'Compras', 'Registrar compras y productos adquiridos'),
    ('CMP_COMPRAS_EDITAR', 'Editar compras', 'Compras', 'Modificar compras mientras su estado lo permita'),
    ('CMP_COMPRAS_ANULAR', 'Anular compras', 'Compras', 'Anular compras registradas')
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
      'CMP_COMPRAS_VER',
      'CMP_COMPRAS_CREAR',
      'CMP_COMPRAS_EDITAR',
      'CMP_COMPRAS_ANULAR'
  )
ON CONFLICT DO NOTHING;
