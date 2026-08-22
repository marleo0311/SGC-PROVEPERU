ALTER TABLE devolucion DROP CONSTRAINT ck_devolucion_resolucion;
ALTER TABLE devolucion DROP CONSTRAINT ck_devolucion_importes;

ALTER TABLE devolucion
    ADD COLUMN id_usuario_resolucion BIGINT,
    ADD COLUMN id_metodo_pago_resolucion BIGINT,
    ADD COLUMN fecha_resolucion TIMESTAMPTZ,
    ADD COLUMN referencia_resolucion VARCHAR(120),
    ADD COLUMN importe_reemplazo NUMERIC(14,2) NOT NULL DEFAULT 0,
    ADD COLUMN importe_cobrado NUMERIC(14,2) NOT NULL DEFAULT 0;

ALTER TABLE devolucion
    ADD CONSTRAINT fk_devolucion_usuario_resolucion
        FOREIGN KEY (id_usuario_resolucion) REFERENCES usuario (id_usuario),
    ADD CONSTRAINT fk_devolucion_metodo_resolucion
        FOREIGN KEY (id_metodo_pago_resolucion) REFERENCES metodo_pago (id_metodo_pago),
    ADD CONSTRAINT ck_devolucion_importes CHECK (
        importe_total >= 0
        AND importe_aplicado_saldo >= 0
        AND importe_reembolsable >= 0
        AND importe_reembolsado >= 0
        AND importe_reemplazo >= 0
        AND importe_cobrado >= 0
        AND importe_aplicado_saldo + importe_reembolsable <= importe_total
        AND importe_reembolsado <= importe_reembolsable
    );

ALTER TABLE devolucion DROP CONSTRAINT ck_devolucion_estado;
ALTER TABLE devolucion ADD CONSTRAINT ck_devolucion_estado CHECK (
    estado IN (
        'PENDIENTE_REEMBOLSO', 'REEMBOLSADA', 'COMPLETADA',
        'PENDIENTE_CAMBIO', 'CAMBIADA',
        'PENDIENTE_DESCUENTO', 'DESCONTADA'
    )
);

ALTER TABLE devolucion ADD CONSTRAINT ck_devolucion_resolucion CHECK (
    (
        tipo_solucion = 'REEMBOLSO'
        AND id_usuario_resolucion IS NULL
        AND id_metodo_pago_resolucion IS NULL
        AND fecha_resolucion IS NULL
        AND referencia_resolucion IS NULL
        AND importe_reemplazo = 0
        AND importe_cobrado = 0
        AND importe_total = importe_aplicado_saldo + importe_reembolsable
        AND (
            (estado = 'PENDIENTE_REEMBOLSO'
                AND importe_reembolsable > importe_reembolsado)
            OR (estado = 'REEMBOLSADA'
                AND importe_reembolsable > 0
                AND importe_reembolsable = importe_reembolsado)
            OR (estado = 'COMPLETADA'
                AND importe_reembolsable = 0
                AND importe_reembolsado = 0)
        )
    )
    OR (
        tipo_solucion = 'CAMBIO'
        AND (
            (estado = 'PENDIENTE_CAMBIO'
                AND id_usuario_resolucion IS NULL
                AND id_metodo_pago_resolucion IS NULL
                AND fecha_resolucion IS NULL
                AND referencia_resolucion IS NULL
                AND importe_aplicado_saldo = 0
                AND importe_reembolsable = 0
                AND importe_reembolsado = 0
                AND importe_reemplazo = 0
                AND importe_cobrado = 0)
            OR (estado = 'CAMBIADA'
                AND id_usuario_resolucion IS NOT NULL
                AND fecha_resolucion IS NOT NULL
                AND importe_reembolsable = importe_reembolsado
                AND (
                    (importe_reemplazo > importe_total
                        AND importe_cobrado = importe_reemplazo - importe_total
                        AND importe_aplicado_saldo = 0
                        AND importe_reembolsable = 0
                        AND id_metodo_pago_resolucion IS NOT NULL)
                    OR (importe_reemplazo = importe_total
                        AND importe_cobrado = 0
                        AND importe_aplicado_saldo = 0
                        AND importe_reembolsable = 0
                        AND id_metodo_pago_resolucion IS NULL)
                    OR (importe_reemplazo < importe_total
                        AND importe_cobrado = 0
                        AND importe_aplicado_saldo + importe_reembolsable
                            = importe_total - importe_reemplazo
                        AND (
                            (importe_reembolsable = 0
                                AND id_metodo_pago_resolucion IS NULL)
                            OR (importe_reembolsable > 0
                                AND id_metodo_pago_resolucion IS NOT NULL)
                        ))
                ))
        )
    )
    OR (
        tipo_solucion = 'DESCUENTO'
        AND importe_reemplazo = 0
        AND importe_cobrado = 0
        AND (
            (estado = 'PENDIENTE_DESCUENTO'
                AND id_usuario_resolucion IS NULL
                AND id_metodo_pago_resolucion IS NULL
                AND fecha_resolucion IS NULL
                AND referencia_resolucion IS NULL
                AND importe_aplicado_saldo = 0
                AND importe_reembolsable = 0
                AND importe_reembolsado = 0)
            OR (estado = 'DESCONTADA'
                AND id_usuario_resolucion IS NOT NULL
                AND fecha_resolucion IS NOT NULL
                AND importe_aplicado_saldo + importe_reembolsable > 0
                AND importe_aplicado_saldo + importe_reembolsable <= importe_total
                AND importe_reembolsable = importe_reembolsado
                AND (
                    (importe_reembolsable = 0
                        AND id_metodo_pago_resolucion IS NULL)
                    OR (importe_reembolsable > 0
                        AND id_metodo_pago_resolucion IS NOT NULL)
                ))
        )
    )
);

CREATE INDEX idx_devolucion_usuario_resolucion_fecha
    ON devolucion (id_usuario_resolucion, fecha_resolucion DESC);

CREATE TABLE detalle_cambio_devolucion (
    id_detalle_cambio BIGSERIAL PRIMARY KEY,
    id_devolucion BIGINT NOT NULL,
    id_producto BIGINT NOT NULL,
    id_unidad_medida BIGINT NOT NULL,
    cantidad NUMERIC(14,3) NOT NULL,
    cantidad_base NUMERIC(14,3) NOT NULL,
    precio_unitario NUMERIC(14,2) NOT NULL,
    subtotal NUMERIC(14,2) NOT NULL,

    CONSTRAINT fk_detalle_cambio_devolucion
        FOREIGN KEY (id_devolucion) REFERENCES devolucion (id_devolucion),
    CONSTRAINT fk_detalle_cambio_producto
        FOREIGN KEY (id_producto) REFERENCES producto (id_producto),
    CONSTRAINT fk_detalle_cambio_unidad
        FOREIGN KEY (id_unidad_medida) REFERENCES unidad_medida (id_unidad_medida),
    CONSTRAINT uq_detalle_cambio_item
        UNIQUE (id_devolucion, id_producto, id_unidad_medida),
    CONSTRAINT ck_detalle_cambio_cantidades CHECK (
        cantidad > 0 AND cantidad_base > 0
    ),
    CONSTRAINT ck_detalle_cambio_importes CHECK (
        precio_unitario > 0
        AND subtotal = round(cantidad * precio_unitario, 2)
    )
);

CREATE INDEX idx_detalle_cambio_devolucion
    ON detalle_cambio_devolucion (id_devolucion);
CREATE INDEX idx_detalle_cambio_producto
    ON detalle_cambio_devolucion (id_producto);

ALTER TABLE movimiento_caja DROP CONSTRAINT ck_movimiento_caja_concepto;
ALTER TABLE movimiento_caja ADD CONSTRAINT ck_movimiento_caja_concepto CHECK (
    concepto IN (
        'VENTA', 'PAGO_CLIENTE', 'INGRESO_MANUAL',
        'EGRESO_MANUAL', 'GASTO', 'PAGO_PROVEEDOR', 'REEMBOLSO',
        'CAMBIO_COBRO', 'CAMBIO_REEMBOLSO', 'DESCUENTO_REEMBOLSO'
    )
);

CREATE UNIQUE INDEX uq_movimiento_caja_cambio_cobro
    ON movimiento_caja (concepto, id_origen)
    WHERE concepto = 'CAMBIO_COBRO';
CREATE UNIQUE INDEX uq_movimiento_caja_cambio_reembolso
    ON movimiento_caja (concepto, id_origen)
    WHERE concepto = 'CAMBIO_REEMBOLSO';
CREATE UNIQUE INDEX uq_movimiento_caja_descuento_reembolso
    ON movimiento_caja (concepto, id_origen)
    WHERE concepto = 'DESCUENTO_REEMBOLSO';

INSERT INTO permiso (codigo, nombre, modulo, descripcion)
VALUES
    ('DEV_CAMBIOS_CREAR', 'Registrar cambios', 'Devoluciones', 'Entregar productos de reemplazo, validar stock y registrar Kardex y diferencias económicas'),
    ('DEV_DESCUENTOS_APLICAR', 'Autorizar descuentos postventa', 'Devoluciones', 'Autorizar descuentos para que el cliente conserve un producto defectuoso o dañado')
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
      'DEV_CAMBIOS_CREAR',
      'DEV_DESCUENTOS_APLICAR'
  )
ON CONFLICT DO NOTHING;
