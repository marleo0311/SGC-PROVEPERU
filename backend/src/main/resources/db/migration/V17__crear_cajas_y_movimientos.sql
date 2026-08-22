CREATE TABLE caja (
    id_caja BIGSERIAL PRIMARY KEY,
    id_sede BIGINT NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',

    CONSTRAINT fk_caja_sede
        FOREIGN KEY (id_sede) REFERENCES sede (id_sede),
    CONSTRAINT uq_caja_sede_nombre UNIQUE (id_sede, nombre),
    CONSTRAINT ck_caja_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE INDEX idx_caja_sede_estado ON caja (id_sede, estado);

INSERT INTO caja (id_sede, nombre, estado)
SELECT s.id_sede, 'Caja principal', 'ACTIVO'
FROM sede s
WHERE s.estado = 'ACTIVO'
ON CONFLICT (id_sede, nombre) DO NOTHING;

CREATE TABLE sesion_caja (
    id_sesion_caja BIGSERIAL PRIMARY KEY,
    id_caja BIGINT NOT NULL,
    id_usuario_apertura BIGINT NOT NULL,
    id_usuario_cierre BIGINT,
    fecha_hora_apertura TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    saldo_inicial NUMERIC(14,2) NOT NULL,
    fecha_hora_cierre TIMESTAMPTZ,
    saldo_esperado NUMERIC(14,2),
    saldo_real NUMERIC(14,2),
    diferencia NUMERIC(14,2),
    observacion_cierre VARCHAR(300),
    estado VARCHAR(20) NOT NULL DEFAULT 'ABIERTA',

    CONSTRAINT fk_sesion_caja_caja
        FOREIGN KEY (id_caja) REFERENCES caja (id_caja),
    CONSTRAINT fk_sesion_caja_usuario_apertura
        FOREIGN KEY (id_usuario_apertura) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_sesion_caja_usuario_cierre
        FOREIGN KEY (id_usuario_cierre) REFERENCES usuario (id_usuario),
    CONSTRAINT ck_sesion_caja_saldo_inicial CHECK (saldo_inicial >= 0),
    CONSTRAINT ck_sesion_caja_estado CHECK (estado IN ('ABIERTA', 'CERRADA')),
    CONSTRAINT ck_sesion_caja_cierre CHECK (
        (estado = 'ABIERTA'
            AND id_usuario_cierre IS NULL
            AND fecha_hora_cierre IS NULL
            AND saldo_esperado IS NULL
            AND saldo_real IS NULL
            AND diferencia IS NULL)
        OR
        (estado = 'CERRADA'
            AND id_usuario_cierre IS NOT NULL
            AND fecha_hora_cierre IS NOT NULL
            AND saldo_esperado IS NOT NULL
            AND saldo_real IS NOT NULL
            AND saldo_real >= 0
            AND diferencia = saldo_real - saldo_esperado)
    )
);

CREATE UNIQUE INDEX uq_sesion_caja_abierta_por_caja
    ON sesion_caja (id_caja)
    WHERE estado = 'ABIERTA';
CREATE UNIQUE INDEX uq_sesion_caja_abierta_por_usuario
    ON sesion_caja (id_usuario_apertura)
    WHERE estado = 'ABIERTA';
CREATE INDEX idx_sesion_caja_fecha
    ON sesion_caja (fecha_hora_apertura DESC);

CREATE TABLE movimiento_caja (
    id_movimiento_caja BIGSERIAL PRIMARY KEY,
    id_sesion_caja BIGINT NOT NULL,
    id_metodo_pago BIGINT NOT NULL,
    id_usuario BIGINT NOT NULL,
    id_venta BIGINT,
    id_pago_cliente BIGINT,
    id_vendedor BIGINT,
    tipo VARCHAR(20) NOT NULL,
    concepto VARCHAR(40) NOT NULL,
    id_origen BIGINT,
    importe NUMERIC(14,2) NOT NULL,
    referencia VARCHAR(120),
    observacion VARCHAR(300),
    fecha_hora TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_movimiento_caja_sesion
        FOREIGN KEY (id_sesion_caja) REFERENCES sesion_caja (id_sesion_caja),
    CONSTRAINT fk_movimiento_caja_metodo
        FOREIGN KEY (id_metodo_pago) REFERENCES metodo_pago (id_metodo_pago),
    CONSTRAINT fk_movimiento_caja_usuario
        FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario),
    CONSTRAINT fk_movimiento_caja_venta
        FOREIGN KEY (id_venta) REFERENCES venta (id_venta),
    CONSTRAINT fk_movimiento_caja_pago_cliente
        FOREIGN KEY (id_pago_cliente) REFERENCES pago_cliente (id_pago_cliente),
    CONSTRAINT fk_movimiento_caja_vendedor
        FOREIGN KEY (id_vendedor) REFERENCES usuario (id_usuario),
    CONSTRAINT ck_movimiento_caja_tipo CHECK (tipo IN ('INGRESO', 'EGRESO')),
    CONSTRAINT ck_movimiento_caja_concepto CHECK (
        concepto IN (
            'VENTA', 'PAGO_CLIENTE', 'INGRESO_MANUAL',
            'EGRESO_MANUAL', 'GASTO', 'PAGO_PROVEEDOR', 'REEMBOLSO'
        )
    ),
    CONSTRAINT ck_movimiento_caja_importe CHECK (importe > 0),
    CONSTRAINT ck_movimiento_caja_manual CHECK (
        (concepto = 'INGRESO_MANUAL' AND tipo = 'INGRESO')
        OR (concepto = 'EGRESO_MANUAL' AND tipo = 'EGRESO')
        OR concepto NOT IN ('INGRESO_MANUAL', 'EGRESO_MANUAL')
    )
);

CREATE INDEX idx_movimiento_caja_sesion_fecha
    ON movimiento_caja (id_sesion_caja, fecha_hora DESC);
CREATE INDEX idx_movimiento_caja_usuario_fecha
    ON movimiento_caja (id_usuario, fecha_hora DESC);
CREATE INDEX idx_movimiento_caja_metodo_fecha
    ON movimiento_caja (id_metodo_pago, fecha_hora DESC);
CREATE INDEX idx_movimiento_caja_vendedor_fecha
    ON movimiento_caja (id_vendedor, fecha_hora DESC);
CREATE UNIQUE INDEX uq_movimiento_caja_venta
    ON movimiento_caja (concepto, id_origen)
    WHERE concepto = 'VENTA';
CREATE UNIQUE INDEX uq_movimiento_caja_pago_cliente
    ON movimiento_caja (concepto, id_origen)
    WHERE concepto = 'PAGO_CLIENTE';

INSERT INTO permiso (codigo, nombre, modulo, descripcion)
VALUES
    ('CAJ_CAJAS_VER', 'Consultar cajas', 'Caja', 'Listar las cajas disponibles por sede'),
    ('CAJ_SESIONES_ABRIR', 'Abrir caja', 'Caja', 'Abrir una sesión de caja con saldo inicial'),
    ('CAJ_MOVIMIENTOS_VER', 'Consultar movimientos de caja', 'Caja', 'Consultar movimientos y sus referencias de origen'),
    ('CAJ_MOVIMIENTOS_CREAR', 'Registrar movimientos manuales', 'Caja', 'Registrar ingresos y egresos manuales autorizados'),
    ('CAJ_SESIONES_CERRAR', 'Cerrar caja', 'Caja', 'Cerrar una sesión registrando el efectivo real'),
    ('CAJ_RESUMEN_VER', 'Consultar resumen de caja', 'Caja', 'Consultar totales y saldo esperado por método de pago')
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
      'CAJ_CAJAS_VER',
      'CAJ_SESIONES_ABRIR',
      'CAJ_MOVIMIENTOS_VER',
      'CAJ_MOVIMIENTOS_CREAR',
      'CAJ_SESIONES_CERRAR',
      'CAJ_RESUMEN_VER'
  )
ON CONFLICT DO NOTHING;
