-- Datos contrastados con la Ficha RUC emitida por SUNAT el 24/08/2026.
-- El ubigeo 140106 corresponde a La Victoria, Chiclayo, Lambayeque.
DO $$
DECLARE
    empresa_actualizada BIGINT;
BEGIN
    UPDATE empresa
    SET razon_social = 'INVERSIONES PROVEPERU S.R.L.',
        nombre_comercial = NULL,
        direccion = 'CAL. MAYTA CAPAC NRO. 1633 URB. LA VICTORIA SC. TRES PARC. A',
        telefono = '979291560',
        ubigeo = '140106',
        departamento = 'LAMBAYEQUE',
        provincia = 'CHICLAYO',
        distrito = 'LA VICTORIA',
        codigo_pais = 'PE',
        estado = 'ACTIVO'
    WHERE ruc = '20612296911'
    RETURNING id_empresa INTO empresa_actualizada;

    IF empresa_actualizada IS NULL THEN
        RAISE EXCEPTION 'No existe la empresa PROVEPERU que debe recibir los datos fiscales';
    END IF;

    UPDATE sede
    SET direccion = 'CAL. MAYTA CAPAC NRO. 1633 URB. LA VICTORIA SC. TRES PARC. A',
        codigo_establecimiento_sunat = '0000'
    WHERE id_empresa = empresa_actualizada
      AND nombre = 'Sede Principal';

    IF NOT FOUND THEN
        RAISE EXCEPTION 'No existe la Sede Principal de PROVEPERU';
    END IF;
END $$;
