CREATE TABLE empresa (
    id_empresa BIGSERIAL PRIMARY KEY,
    ruc VARCHAR(11) NOT NULL,
    razon_social VARCHAR(200) NOT NULL,
    nombre_comercial VARCHAR(200),
    direccion VARCHAR(250),
    telefono VARCHAR(30),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',

    CONSTRAINT uq_empresa_ruc UNIQUE (ruc),
    CONSTRAINT ck_empresa_ruc_formato CHECK (ruc ~ '^[0-9]{11}$'),
    CONSTRAINT ck_empresa_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE TABLE sede (
    id_sede BIGSERIAL PRIMARY KEY,
    id_empresa BIGINT NOT NULL,
    nombre VARCHAR(120) NOT NULL,
    direccion VARCHAR(250),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',

    CONSTRAINT fk_sede_empresa
        FOREIGN KEY (id_empresa) REFERENCES empresa (id_empresa),
    CONSTRAINT uq_sede_empresa_nombre UNIQUE (id_empresa, nombre),
    CONSTRAINT ck_sede_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE TABLE metodo_pago (
    id_metodo_pago BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL,
    nombre VARCHAR(80) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',

    CONSTRAINT uq_metodo_pago_codigo UNIQUE (codigo),
    CONSTRAINT ck_metodo_pago_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);
