ALTER TABLE venta DROP CONSTRAINT ck_venta_estado;
ALTER TABLE venta ADD CONSTRAINT ck_venta_estado CHECK (
    estado IN ('REGISTRADA', 'ANULADA', 'DEVUELTA_PARCIAL', 'DEVUELTA_TOTAL')
);

ALTER TABLE cuenta_cobrar DROP CONSTRAINT ck_cuenta_cobrar_importes;
ALTER TABLE cuenta_cobrar ADD CONSTRAINT ck_cuenta_cobrar_importes CHECK (
    total >= 0
    AND importe_pagado >= 0
    AND importe_pagado <= total
    AND saldo_pendiente >= 0
    AND (
        (estado = 'ANULADO' AND saldo_pendiente = 0)
        OR (estado <> 'ANULADO'
            AND saldo_pendiente = total - importe_pagado)
    )
);

CREATE TABLE devolucion (
    id_devolucion BIGSERIAL PRIMARY KEY,
    id_venta BIGINT NOT NULL,
    id_usuario BIGINT NOT NULL,
    fecha_hora TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    motivo VARCHAR(300) NOT NULL,
    tipo_solucion VARCHAR(30) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    importe_total NUMERIC(14,2) NOT NULL,
    importe_aplicado_saldo NUMERIC(14,2) NOT NULL DEFAULT 0,
    importe_reembolsable NUMERIC(14,2) NOT NULL DEFAULT 0,
    importe_reembolsado NUMERIC(14,2) NOT NULL DEFAULT 0,

    CONSTRAINT fk_devolucion_venta
        FOREIGN KEY (id_venta) REFERENCES venta (id_venta),
    CONSTRAINT fk_devolucion_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT ck_devolucion_solucion CHECK (
        tipo_solucion IN ('DESCUENTO', 'CAMBIO', 'REEMBOLSO')
    ),
    CONSTRAINT ck_devolucion_estado CHECK (
        estado IN ('PENDIENTE_REEMBOLSO', 'REEMBOLSADA', 'COMPLETADA')
    ),
    CONSTRAINT ck_devolucion_importes CHECK (
        importe_total >= 0
        AND importe_aplicado_saldo >= 0
        AND importe_reembolsable >= 0
        AND importe_reembolsado >= 0
        AND importe_total = importe_aplicado_saldo + importe_reembolsable
        AND importe_reembolsado <= importe_reembolsable
    ),
    CONSTRAINT ck_devolucion_resolucion CHECK (
        (estado = 'PENDIENTE_REEMBOLSO'
            AND importe_reembolsable > importe_reembolsado)
        OR (estado = 'REEMBOLSADA'
            AND importe_reembolsable > 0
            AND importe_reembolsable = importe_reembolsado)
        OR (estado = 'COMPLETADA'
            AND importe_reembolsable = 0
            AND importe_reembolsado = 0)
    )
);

CREATE INDEX idx_devolucion_venta_fecha
    ON devolucion (id_venta, fecha_hora DESC);
CREATE INDEX idx_devolucion_estado_fecha
    ON devolucion (estado, fecha_hora DESC);
CREATE INDEX idx_devolucion_usuario_fecha
    ON devolucion (id_usuario, fecha_hora DESC);

CREATE TABLE detalle_devolucion (
    id_detalle_devolucion BIGSERIAL PRIMARY KEY,
    id_devolucion BIGINT NOT NULL,
    id_detalle_venta BIGINT NOT NULL,
    id_producto BIGINT NOT NULL,
    id_unidad_medida BIGINT NOT NULL,
    cantidad NUMERIC(14,3) NOT NULL,
    cantidad_base NUMERIC(14,3) NOT NULL,
    estado_producto VARCHAR(30) NOT NULL,
    importe_devolucion NUMERIC(14,2) NOT NULL,
    importe_reembolso NUMERIC(14,2) NOT NULL DEFAULT 0,
    descuento_aplicado NUMERIC(14,2) NOT NULL DEFAULT 0,

    CONSTRAINT fk_detalle_devolucion_devolucion
        FOREIGN KEY (id_devolucion) REFERENCES devolucion (id_devolucion),
    CONSTRAINT fk_detalle_devolucion_venta
        FOREIGN KEY (id_detalle_venta) REFERENCES detalle_venta (id_detalle_venta),
    CONSTRAINT fk_detalle_devolucion_producto
        FOREIGN KEY (id_producto) REFERENCES producto (id_producto),
    CONSTRAINT fk_detalle_devolucion_unidad
        FOREIGN KEY (id_unidad_medida) REFERENCES unidad_medida (id_unidad_medida),
    CONSTRAINT uq_detalle_devolucion_item
        UNIQUE (id_devolucion, id_detalle_venta),
    CONSTRAINT ck_detalle_devolucion_cantidades CHECK (
        cantidad > 0 AND cantidad_base > 0
    ),
    CONSTRAINT ck_detalle_devolucion_estado_producto CHECK (
        estado_producto IN ('APTO', 'DEFECTUOSO', 'DANADO', 'PENDIENTE')
    ),
    CONSTRAINT ck_detalle_devolucion_importes CHECK (
        importe_devolucion >= 0
        AND importe_reembolso >= 0
        AND descuento_aplicado >= 0
    )
);

CREATE INDEX idx_detalle_devolucion_devolucion
    ON detalle_devolucion (id_devolucion);
CREATE INDEX idx_detalle_devolucion_venta
    ON detalle_devolucion (id_detalle_venta);
CREATE INDEX idx_detalle_devolucion_producto
    ON detalle_devolucion (id_producto);

CREATE TABLE reembolso_devolucion (
    id_reembolso_devolucion BIGSERIAL PRIMARY KEY,
    id_devolucion BIGINT NOT NULL,
    id_metodo_pago BIGINT NOT NULL,
    id_usuario BIGINT NOT NULL,
    importe NUMERIC(14,2) NOT NULL,
    referencia VARCHAR(120),
    fecha_hora TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reembolso_devolucion_devolucion
        FOREIGN KEY (id_devolucion) REFERENCES devolucion (id_devolucion),
    CONSTRAINT fk_reembolso_devolucion_metodo
        FOREIGN KEY (id_metodo_pago) REFERENCES metodo_pago (id_metodo_pago),
    CONSTRAINT fk_reembolso_devolucion_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT uq_reembolso_devolucion UNIQUE (id_devolucion),
    CONSTRAINT ck_reembolso_devolucion_importe CHECK (importe > 0)
);

CREATE INDEX idx_reembolso_devolucion_fecha
    ON reembolso_devolucion (fecha_hora DESC);

CREATE UNIQUE INDEX uq_movimiento_caja_reembolso
    ON movimiento_caja (concepto, id_origen)
    WHERE concepto = 'REEMBOLSO';

INSERT INTO permiso (codigo, nombre, modulo, descripcion)
VALUES
    ('DEV_DEVOLUCIONES_VER', 'Consultar devoluciones', 'Devoluciones', 'Listar devoluciones y consultar su clasificación, importes y trazabilidad'),
    ('DEV_DEVOLUCIONES_CREAR', 'Registrar devoluciones', 'Devoluciones', 'Registrar productos devueltos y reincorporar únicamente los aptos al inventario'),
    ('DEV_REEMBOLSOS_CREAR', 'Registrar reembolsos', 'Devoluciones', 'Devolver dinero al cliente y generar el egreso correspondiente en caja')
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
      'DEV_DEVOLUCIONES_VER',
      'DEV_DEVOLUCIONES_CREAR',
      'DEV_REEMBOLSOS_CREAR'
  )
ON CONFLICT DO NOTHING;
