ALTER TABLE sede
    ADD COLUMN es_sede_facturacion BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE sede
SET es_sede_facturacion = TRUE
WHERE id_sede = (
    SELECT id_sede
    FROM sede
    WHERE estado = 'ACTIVO'
    ORDER BY id_sede
    LIMIT 1
);

UPDATE sede
SET nombre = 'Almacén de Tienda'
WHERE nombre = 'Sede Principal'
  AND NOT EXISTS (
      SELECT 1
      FROM sede existente
      WHERE existente.id_empresa = sede.id_empresa
        AND existente.nombre = 'Almacén de Tienda'
  );

INSERT INTO sede (
    id_empresa,
    nombre,
    direccion,
    codigo_establecimiento_sunat,
    es_sede_facturacion,
    estado
)
SELECT
    id_empresa,
    'Almacén General',
    direccion,
    codigo_establecimiento_sunat,
    FALSE,
    'ACTIVO'
FROM sede
WHERE es_sede_facturacion = TRUE
ORDER BY id_sede
LIMIT 1
ON CONFLICT (id_empresa, nombre) DO UPDATE SET
    estado = 'ACTIVO',
    es_sede_facturacion = FALSE;

ALTER TABLE inventario
    ADD COLUMN stock_minimo NUMERIC(14,3);

UPDATE inventario inventario_actual
SET stock_minimo = producto.stock_minimo
FROM producto
WHERE producto.id_producto = inventario_actual.id_producto;

ALTER TABLE inventario
    ALTER COLUMN stock_minimo SET DEFAULT 0,
    ALTER COLUMN stock_minimo SET NOT NULL,
    ADD CONSTRAINT ck_inventario_stock_minimo CHECK (stock_minimo >= 0);

INSERT INTO inventario (
    id_sede,
    id_producto,
    stock_fisico,
    stock_reservado,
    stock_minimo
)
SELECT
    sede.id_sede,
    producto.id_producto,
    0,
    0,
    producto.stock_minimo
FROM sede
CROSS JOIN producto
WHERE sede.estado = 'ACTIVO'
ON CONFLICT (id_sede, id_producto) DO NOTHING;

CREATE TABLE transferencia_inventario (
    id_transferencia BIGSERIAL PRIMARY KEY,
    id_sede_origen BIGINT NOT NULL,
    id_sede_destino BIGINT NOT NULL,
    id_producto BIGINT NOT NULL,
    id_unidad_medida BIGINT NOT NULL,
    id_usuario BIGINT NOT NULL,
    cantidad NUMERIC(14,3) NOT NULL,
    cantidad_base NUMERIC(14,3) NOT NULL,
    motivo VARCHAR(250) NOT NULL,
    fecha_hora TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_transferencia_sede_origen
        FOREIGN KEY (id_sede_origen) REFERENCES sede (id_sede),
    CONSTRAINT fk_transferencia_sede_destino
        FOREIGN KEY (id_sede_destino) REFERENCES sede (id_sede),
    CONSTRAINT fk_transferencia_producto
        FOREIGN KEY (id_producto) REFERENCES producto (id_producto),
    CONSTRAINT fk_transferencia_unidad
        FOREIGN KEY (id_unidad_medida) REFERENCES unidad_medida (id_unidad_medida),
    CONSTRAINT fk_transferencia_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT ck_transferencia_sedes_distintas
        CHECK (id_sede_origen <> id_sede_destino),
    CONSTRAINT ck_transferencia_cantidad
        CHECK (cantidad > 0 AND cantidad_base > 0)
);

CREATE INDEX idx_transferencia_fecha
    ON transferencia_inventario (fecha_hora DESC);
CREATE INDEX idx_transferencia_producto
    ON transferencia_inventario (id_producto, fecha_hora DESC);

ALTER TABLE venta
    ADD COLUMN id_almacen_salida BIGINT;

UPDATE venta
SET id_almacen_salida = id_sede;

ALTER TABLE venta
    ALTER COLUMN id_almacen_salida SET NOT NULL,
    ADD CONSTRAINT fk_venta_almacen_salida
        FOREIGN KEY (id_almacen_salida) REFERENCES sede (id_sede);

INSERT INTO permiso (codigo, nombre, modulo, descripcion)
VALUES
    (
        'INV_TRANSFERENCIAS_CREAR',
        'Transferir existencias',
        'Inventario',
        'Mover mercadería entre almacenes con trazabilidad completa'
    ),
    (
        'INV_MINIMOS_EDITAR',
        'Configurar mínimos por almacén',
        'Inventario',
        'Definir el nivel mínimo y las alertas de cada producto por almacén'
    )
ON CONFLICT (codigo) DO UPDATE SET
    nombre = EXCLUDED.nombre,
    modulo = EXCLUDED.modulo,
    descripcion = EXCLUDED.descripcion;

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT rol.id_rol, permiso.id_permiso
FROM rol
CROSS JOIN permiso
WHERE lower(rol.nombre) = lower('Administrador')
  AND permiso.codigo IN ('INV_TRANSFERENCIAS_CREAR', 'INV_MINIMOS_EDITAR')
ON CONFLICT DO NOTHING;

COMMENT ON COLUMN sede.es_sede_facturacion IS
    'Identifica el único local fiscal usado para comprobantes; los demás registros son almacenes internos';
COMMENT ON COLUMN venta.id_almacen_salida IS
    'Almacén interno del cual se descontó físicamente la mercadería';
