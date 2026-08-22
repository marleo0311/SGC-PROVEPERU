CREATE TABLE recepcion_compra (
    id_recepcion BIGSERIAL PRIMARY KEY,
    id_compra BIGINT NOT NULL,
    id_sede BIGINT NOT NULL,
    id_usuario BIGINT NOT NULL,
    fecha_hora TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observacion VARCHAR(300),
    estado VARCHAR(30) NOT NULL,

    CONSTRAINT fk_recepcion_compra_compra
        FOREIGN KEY (id_compra) REFERENCES compra (id_compra),
    CONSTRAINT fk_recepcion_compra_sede
        FOREIGN KEY (id_sede) REFERENCES sede (id_sede),
    CONSTRAINT fk_recepcion_compra_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT ck_recepcion_compra_estado CHECK (
        estado IN ('PENDIENTE', 'CONFIRMADA', 'CON_INCIDENCIA')
    )
);

CREATE INDEX idx_recepcion_compra_compra_fecha
    ON recepcion_compra (id_compra, fecha_hora DESC);
CREATE INDEX idx_recepcion_compra_sede_fecha
    ON recepcion_compra (id_sede, fecha_hora DESC);

CREATE TABLE detalle_recepcion_compra (
    id_detalle_recepcion BIGSERIAL PRIMARY KEY,
    id_recepcion BIGINT NOT NULL,
    id_detalle_compra BIGINT NOT NULL,
    id_producto BIGINT NOT NULL,
    id_unidad_medida BIGINT NOT NULL,
    cantidad_esperada NUMERIC(14,3) NOT NULL,
    cantidad_recibida NUMERIC(14,3) NOT NULL,
    cantidad_acumulada NUMERIC(14,3) NOT NULL,
    cantidad_pendiente NUMERIC(14,3) NOT NULL,
    conforme BOOLEAN NOT NULL,
    observacion VARCHAR(250),

    CONSTRAINT fk_detalle_recepcion_recepcion
        FOREIGN KEY (id_recepcion) REFERENCES recepcion_compra (id_recepcion),
    CONSTRAINT fk_detalle_recepcion_detalle_compra
        FOREIGN KEY (id_detalle_compra) REFERENCES detalle_compra (id_detalle_compra),
    CONSTRAINT fk_detalle_recepcion_producto
        FOREIGN KEY (id_producto) REFERENCES producto (id_producto),
    CONSTRAINT fk_detalle_recepcion_unidad
        FOREIGN KEY (id_unidad_medida) REFERENCES unidad_medida (id_unidad_medida),
    CONSTRAINT uq_detalle_recepcion_item
        UNIQUE (id_recepcion, id_detalle_compra),
    CONSTRAINT ck_detalle_recepcion_cantidades CHECK (
        cantidad_esperada > 0
        AND cantidad_recibida > 0
        AND cantidad_acumulada > 0
        AND cantidad_pendiente >= 0
        AND cantidad_acumulada <= cantidad_esperada
        AND cantidad_pendiente = cantidad_esperada - cantidad_acumulada
    )
);

CREATE INDEX idx_detalle_recepcion_recepcion
    ON detalle_recepcion_compra (id_recepcion);
CREATE INDEX idx_detalle_recepcion_detalle_compra
    ON detalle_recepcion_compra (id_detalle_compra);
CREATE INDEX idx_movimiento_inventario_origen
    ON movimiento_inventario (documento_origen, id_origen)
    WHERE documento_origen IS NOT NULL AND id_origen IS NOT NULL;

INSERT INTO permiso (codigo, nombre, modulo, descripcion)
VALUES
    ('CMP_RECEPCIONES_VER', 'Consultar recepciones de compra', 'Compras', 'Consultar recepciones y diferencias de mercadería'),
    ('CMP_RECEPCIONES_CREAR', 'Confirmar recepciones de compra', 'Compras', 'Registrar recepciones parciales o totales con ingreso a inventario y Kardex')
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
      'CMP_RECEPCIONES_VER',
      'CMP_RECEPCIONES_CREAR'
  )
ON CONFLICT DO NOTHING;
