ALTER TABLE presentacion_producto
    ADD COLUMN precio_minorista NUMERIC(14,2),
    ADD COLUMN precio_mayorista NUMERIC(14,2),
    ADD CONSTRAINT ck_presentacion_producto_precio_minorista
        CHECK (precio_minorista IS NULL OR precio_minorista > 0),
    ADD CONSTRAINT ck_presentacion_producto_precio_mayorista
        CHECK (precio_mayorista IS NULL OR precio_mayorista > 0);

COMMENT ON COLUMN presentacion_producto.precio_minorista IS
    'Precio minorista opcional del bulto cerrado; si es nulo se calcula desde la unidad base';
COMMENT ON COLUMN presentacion_producto.precio_mayorista IS
    'Precio mayorista opcional del bulto cerrado; si es nulo se calcula desde la unidad base';
