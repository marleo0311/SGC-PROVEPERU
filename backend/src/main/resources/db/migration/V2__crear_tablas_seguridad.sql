CREATE TABLE rol (
    id_rol BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(80) NOT NULL,
    descripcion VARCHAR(250),
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',

    CONSTRAINT uq_rol_nombre UNIQUE (nombre),
    CONSTRAINT ck_rol_estado CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE TABLE permiso (
    id_permiso BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(100) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    modulo VARCHAR(80) NOT NULL,
    descripcion VARCHAR(250),

    CONSTRAINT uq_permiso_codigo UNIQUE (codigo)
);

CREATE TABLE rol_permiso (
    id_rol BIGINT NOT NULL,
    id_permiso BIGINT NOT NULL,

    CONSTRAINT pk_rol_permiso PRIMARY KEY (id_rol, id_permiso),
    CONSTRAINT fk_rol_permiso_rol
        FOREIGN KEY (id_rol) REFERENCES rol (id_rol),
    CONSTRAINT fk_rol_permiso_permiso
        FOREIGN KEY (id_permiso) REFERENCES permiso (id_permiso)
);

CREATE TABLE usuario (
    id_usuario BIGSERIAL PRIMARY KEY,
    id_rol BIGINT NOT NULL,
    nombre_completo VARCHAR(180) NOT NULL,
    usuario_login VARCHAR(180) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVO',
    ultimo_acceso TIMESTAMPTZ,
    fecha_registro TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_usuario_rol
        FOREIGN KEY (id_rol) REFERENCES rol (id_rol),
    CONSTRAINT uq_usuario_login UNIQUE (usuario_login),
    CONSTRAINT ck_usuario_estado CHECK (estado IN ('ACTIVO', 'SUSPENDIDO'))
);

CREATE INDEX idx_usuario_id_rol ON usuario (id_rol);
