ALTER TABLE unidad_medida
    DROP CONSTRAINT IF EXISTS ck_unidad_medida_codigo_sunat;

ALTER TABLE unidad_medida
    ADD CONSTRAINT ck_unidad_medida_codigo_sunat CHECK (
        codigo_sunat ~ '^[A-Z0-9]{2,3}$'
    );

UPDATE unidad_medida
SET codigo_sunat = CASE
    WHEN upper(codigo) IN ('UND', 'UNIDAD', 'NIU') THEN 'NIU'
    WHEN upper(codigo) IN ('M', 'MT', 'MTR', 'METRO') THEN 'MTR'
    WHEN upper(codigo) IN ('KG', 'KGM', 'KILO') THEN 'KGM'
    WHEN upper(codigo) IN ('L', 'LT', 'LTR', 'LITRO') THEN 'LTR'
    ELSE codigo_sunat
END;

INSERT INTO unidad_medida (codigo, nombre, codigo_sunat, permite_decimales, estado)
SELECT 'CAJ', 'Caja', 'BX', FALSE, 'ACTIVO'
WHERE NOT EXISTS (SELECT 1 FROM unidad_medida WHERE lower(codigo) = 'caj');

INSERT INTO unidad_medida (codigo, nombre, codigo_sunat, permite_decimales, estado)
SELECT 'PAQ', 'Paquete', 'NIU', FALSE, 'ACTIVO'
WHERE NOT EXISTS (SELECT 1 FROM unidad_medida WHERE lower(codigo) = 'paq');

INSERT INTO unidad_medida (codigo, nombre, codigo_sunat, permite_decimales, estado)
SELECT 'ROL', 'Rollo', 'NIU', FALSE, 'ACTIVO'
WHERE NOT EXISTS (SELECT 1 FROM unidad_medida WHERE lower(codigo) = 'rol');

UPDATE unidad_medida SET codigo_sunat = 'BX', permite_decimales = FALSE
WHERE lower(codigo) = 'caj';
UPDATE unidad_medida SET codigo_sunat = 'NIU', permite_decimales = FALSE
WHERE lower(codigo) IN ('paq', 'rol');

CREATE TABLE presentacion_producto (
    id_presentacion_producto BIGSERIAL PRIMARY KEY,
    id_producto BIGINT NOT NULL,
    id_unidad_medida BIGINT NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    contenido_variable BOOLEAN NOT NULL DEFAULT TRUE,
    contenido_base_predeterminado NUMERIC(14,3),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',

    CONSTRAINT fk_presentacion_producto_producto
        FOREIGN KEY (id_producto) REFERENCES producto (id_producto),
    CONSTRAINT fk_presentacion_producto_unidad
        FOREIGN KEY (id_unidad_medida) REFERENCES unidad_medida (id_unidad_medida),
    CONSTRAINT uq_presentacion_producto_nombre UNIQUE (id_producto, nombre),
    CONSTRAINT uq_presentacion_producto_unidad UNIQUE (id_producto, id_unidad_medida),
    CONSTRAINT ck_presentacion_producto_estado CHECK (estado IN ('ACTIVO', 'INACTIVO')),
    CONSTRAINT ck_presentacion_producto_contenido CHECK (
        (contenido_variable = TRUE AND contenido_base_predeterminado IS NULL)
        OR (contenido_variable = FALSE AND contenido_base_predeterminado > 0)
    )
);

CREATE INDEX idx_presentacion_producto_producto
    ON presentacion_producto (id_producto, estado);

CREATE TABLE existencia_presentacion (
    id_existencia_presentacion BIGSERIAL PRIMARY KEY,
    id_presentacion_producto BIGINT NOT NULL,
    id_sede BIGINT NOT NULL,
    id_recepcion_compra BIGINT,
    codigo VARCHAR(80) UNIQUE,
    cantidad_inicial_base NUMERIC(14,3) NOT NULL,
    cantidad_disponible_base NUMERIC(14,3) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'CERRADO',
    fecha_ingreso TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_apertura TIMESTAMPTZ,
    fecha_agotamiento TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_existencia_presentacion_presentacion
        FOREIGN KEY (id_presentacion_producto)
        REFERENCES presentacion_producto (id_presentacion_producto),
    CONSTRAINT fk_existencia_presentacion_sede
        FOREIGN KEY (id_sede) REFERENCES sede (id_sede),
    CONSTRAINT fk_existencia_presentacion_recepcion
        FOREIGN KEY (id_recepcion_compra)
        REFERENCES recepcion_compra (id_recepcion),
    CONSTRAINT ck_existencia_presentacion_estado CHECK (
        estado IN ('CERRADO', 'ABIERTO', 'AGOTADO')
    ),
    CONSTRAINT ck_existencia_presentacion_cantidades CHECK (
        cantidad_inicial_base > 0
        AND cantidad_disponible_base >= 0
        AND cantidad_disponible_base <= cantidad_inicial_base
    ),
    CONSTRAINT ck_existencia_presentacion_fechas CHECK (
        (estado = 'CERRADO' AND fecha_apertura IS NULL AND fecha_agotamiento IS NULL)
        OR (estado = 'ABIERTO' AND fecha_apertura IS NOT NULL AND fecha_agotamiento IS NULL)
        OR (estado = 'AGOTADO' AND cantidad_disponible_base = 0 AND fecha_agotamiento IS NOT NULL)
    )
);

CREATE INDEX idx_existencia_presentacion_disponible
    ON existencia_presentacion (id_sede, id_presentacion_producto, estado, fecha_ingreso);

ALTER TABLE detalle_venta
    DROP CONSTRAINT IF EXISTS uq_detalle_venta_producto,
    ADD COLUMN id_existencia_presentacion BIGINT,
    ADD CONSTRAINT fk_detalle_venta_existencia_presentacion
        FOREIGN KEY (id_existencia_presentacion)
        REFERENCES existencia_presentacion (id_existencia_presentacion);

CREATE UNIQUE INDEX uq_detalle_venta_existencia_presentacion
    ON detalle_venta (id_existencia_presentacion)
    WHERE id_existencia_presentacion IS NOT NULL;

CREATE TABLE consumo_existencia_presentacion (
    id_consumo_existencia BIGSERIAL PRIMARY KEY,
    id_detalle_venta BIGINT NOT NULL,
    id_existencia_presentacion BIGINT NOT NULL,
    cantidad_base NUMERIC(14,3) NOT NULL,

    CONSTRAINT fk_consumo_existencia_detalle
        FOREIGN KEY (id_detalle_venta) REFERENCES detalle_venta (id_detalle_venta),
    CONSTRAINT fk_consumo_existencia_presentacion
        FOREIGN KEY (id_existencia_presentacion)
        REFERENCES existencia_presentacion (id_existencia_presentacion),
    CONSTRAINT uq_consumo_existencia_detalle_presentacion
        UNIQUE (id_detalle_venta, id_existencia_presentacion),
    CONSTRAINT ck_consumo_existencia_cantidad CHECK (cantidad_base > 0)
);

CREATE INDEX idx_consumo_existencia_detalle
    ON consumo_existencia_presentacion (id_detalle_venta);

INSERT INTO permiso (codigo, nombre, modulo, descripcion)
VALUES
    ('CAT_PRESENTACIONES_VER', 'Consultar presentaciones', 'Catálogo', 'Consultar cajas, paquetes y rollos configurados por producto'),
    ('CAT_PRESENTACIONES_EDITAR', 'Administrar presentaciones', 'Catálogo', 'Configurar las presentaciones comerciales de un producto'),
    ('INV_PRESENTACIONES_GESTIONAR', 'Gestionar bultos', 'Inventario', 'Registrar y abrir cajas, paquetes y rollos con contenido individual')
ON CONFLICT (codigo) DO UPDATE SET
    nombre = EXCLUDED.nombre,
    modulo = EXCLUDED.modulo,
    descripcion = EXCLUDED.descripcion;

INSERT INTO rol_permiso (id_rol, id_permiso)
SELECT rol.id_rol, permiso.id_permiso
FROM rol
CROSS JOIN permiso
WHERE lower(rol.nombre) = lower('Administrador')
  AND permiso.codigo IN (
      'CAT_PRESENTACIONES_VER',
      'CAT_PRESENTACIONES_EDITAR',
      'INV_PRESENTACIONES_GESTIONAR'
  )
ON CONFLICT DO NOTHING;

COMMENT ON TABLE presentacion_producto IS
    'Define cómo se compra o vende un producto: caja, paquete, rollo u otra presentación';
COMMENT ON TABLE existencia_presentacion IS
    'Conserva el contenido real de cada bulto sin duplicar el stock agregado en unidad base';
