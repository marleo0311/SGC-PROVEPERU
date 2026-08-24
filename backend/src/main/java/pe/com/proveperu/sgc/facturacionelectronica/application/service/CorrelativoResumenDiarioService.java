package pe.com.proveperu.sgc.facturacionelectronica.application.service;

import java.sql.Date;
import java.time.LocalDate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.AmbienteSunat;

@Service
public class CorrelativoResumenDiarioService {

    private final JdbcTemplate jdbcTemplate;

    public CorrelativoResumenDiarioService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public int siguiente(AmbienteSunat ambiente, LocalDate fechaDocumentos) {
        Integer correlativo = jdbcTemplate.queryForObject("""
            INSERT INTO correlativo_resumen_diario_sunat (ambiente, fecha_documentos, ultimo)
            VALUES (?, ?, 1)
            ON CONFLICT (ambiente, fecha_documentos)
            DO UPDATE SET ultimo = correlativo_resumen_diario_sunat.ultimo + 1
            RETURNING ultimo
            """, Integer.class, ambiente.name(), Date.valueOf(fechaDocumentos));
        if (correlativo == null || correlativo <= 0 || correlativo > 99999) {
            throw new IllegalStateException("No se pudo reservar el correlativo del resumen diario");
        }
        return correlativo;
    }
}
