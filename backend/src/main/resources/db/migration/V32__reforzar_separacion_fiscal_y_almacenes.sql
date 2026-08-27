CREATE UNIQUE INDEX uq_sede_facturacion_empresa
    ON sede (id_empresa)
    WHERE es_sede_facturacion = TRUE;

CREATE INDEX idx_venta_almacen_salida_fecha
    ON venta (id_almacen_salida, fecha_hora DESC);
