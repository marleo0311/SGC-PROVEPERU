ALTER TABLE sede
    ADD COLUMN codigo_establecimiento_sunat VARCHAR(4) NOT NULL DEFAULT '0000';

ALTER TABLE sede
    ADD CONSTRAINT ck_sede_codigo_establecimiento_sunat CHECK (
        codigo_establecimiento_sunat ~ '^[0-9]{4}$'
    );

COMMENT ON COLUMN sede.codigo_establecimiento_sunat IS
    'Código de establecimiento o local anexo declarado ante SUNAT; 0000 identifica el domicilio fiscal principal';
