INSERT INTO empresa (ruc, razon_social, nombre_comercial, estado)
VALUES ('20612296911', 'INVERSIONES PROVEPERU S.R.L.', 'PROVEPERU', 'ACTIVO')
ON CONFLICT (ruc) DO UPDATE SET
    razon_social = EXCLUDED.razon_social,
    nombre_comercial = EXCLUDED.nombre_comercial;

INSERT INTO sede (id_empresa, nombre, direccion, estado)
SELECT id_empresa, 'Sede Principal', direccion, 'ACTIVO'
FROM empresa
WHERE ruc = '20612296911'
ON CONFLICT (id_empresa, nombre) DO UPDATE SET
    estado = 'ACTIVO';

CREATE TABLE inventario (
    id_inventario BIGSERIAL PRIMARY KEY,
    id_sede BIGINT NOT NULL,
    id_producto BIGINT NOT NULL,
    stock_fisico NUMERIC(14,3) NOT NULL DEFAULT 0,
    stock_reservado NUMERIC(14,3) NOT NULL DEFAULT 0,
    fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_inventario_sede FOREIGN KEY (id_sede) REFERENCES sede (id_sede),
    CONSTRAINT fk_inventario_producto FOREIGN KEY (id_producto) REFERENCES producto (id_producto),
    CONSTRAINT uq_inventario_sede_producto UNIQUE (id_sede, id_producto),
    CONSTRAINT ck_inventario_stock_fisico CHECK (stock_fisico >= 0),
    CONSTRAINT ck_inventario_stock_reservado CHECK (stock_reservado >= 0),
    CONSTRAINT ck_inventario_reserva_disponible CHECK (stock_reservado <= stock_fisico)
);

CREATE INDEX idx_inventario_sede ON inventario (id_sede);
CREATE INDEX idx_inventario_producto ON inventario (id_producto);
CREATE INDEX idx_inventario_stock ON inventario (id_sede, stock_fisico, stock_reservado);

CREATE TABLE movimiento_inventario (
    id_movimiento BIGSERIAL PRIMARY KEY,
    id_sede BIGINT NOT NULL,
    id_producto BIGINT NOT NULL,
    id_usuario BIGINT NOT NULL,
    id_unidad_medida BIGINT NOT NULL,
    tipo_movimiento VARCHAR(40) NOT NULL,
    cantidad NUMERIC(14,3) NOT NULL,
    cantidad_base NUMERIC(14,3) NOT NULL,
    stock_anterior NUMERIC(14,3) NOT NULL,
    stock_resultante NUMERIC(14,3) NOT NULL,
    documento_origen VARCHAR(50),
    id_origen BIGINT,
    motivo VARCHAR(250),
    fecha_hora TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_movimiento_inventario_sede FOREIGN KEY (id_sede) REFERENCES sede (id_sede),
    CONSTRAINT fk_movimiento_inventario_producto FOREIGN KEY (id_producto) REFERENCES producto (id_producto),
    CONSTRAINT fk_movimiento_inventario_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_movimiento_inventario_unidad FOREIGN KEY (id_unidad_medida) REFERENCES unidad_medida (id_unidad_medida),
    CONSTRAINT ck_movimiento_cantidad CHECK (cantidad <> 0),
    CONSTRAINT ck_movimiento_cantidad_base CHECK (cantidad_base <> 0),
    CONSTRAINT ck_movimiento_stock_anterior CHECK (stock_anterior >= 0),
    CONSTRAINT ck_movimiento_stock_resultante CHECK (stock_resultante >= 0),
    CONSTRAINT ck_movimiento_ajuste_motivo CHECK (
        tipo_movimiento NOT IN ('AJUSTE_ENTRADA', 'AJUSTE_SALIDA')
        OR motivo IS NOT NULL
    )
);

CREATE INDEX idx_movimiento_inventario_producto_fecha
    ON movimiento_inventario (id_producto, fecha_hora DESC);
CREATE INDEX idx_movimiento_inventario_sede_fecha
    ON movimiento_inventario (id_sede, fecha_hora DESC);
CREATE INDEX idx_movimiento_inventario_tipo ON movimiento_inventario (tipo_movimiento);

INSERT INTO inventario (id_sede, id_producto)
SELECT s.id_sede, p.id_producto
FROM sede s
CROSS JOIN producto p
WHERE s.estado = 'ACTIVO'
ON CONFLICT (id_sede, id_producto) DO NOTHING;

INSERT INTO permiso (codigo, nombre, modulo, descripcion)
VALUES
    ('INV_STOCK_VER', 'Consultar inventario', 'Inventario', 'Consultar stock físico, reservado, disponible y alertas'),
    ('INV_AJUSTES_CREAR', 'Registrar ajustes de inventario', 'Inventario', 'Registrar entradas y salidas manuales autorizadas'),
    ('INV_MOVIMIENTOS_VER', 'Consultar movimientos de inventario', 'Inventario', 'Consultar movimientos con filtros'),
    ('INV_KARDEX_VER', 'Consultar Kardex', 'Inventario', 'Consultar el historial cronológico de un producto')
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
      'INV_STOCK_VER',
      'INV_AJUSTES_CREAR',
      'INV_MOVIMIENTOS_VER',
      'INV_KARDEX_VER'
  )
ON CONFLICT DO NOTHING;
