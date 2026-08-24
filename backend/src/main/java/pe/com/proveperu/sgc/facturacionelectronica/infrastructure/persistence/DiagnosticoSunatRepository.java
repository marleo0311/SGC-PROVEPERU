package pe.com.proveperu.sgc.facturacionelectronica.infrastructure.persistence;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DiagnosticoSunatRepository {

    private final JdbcTemplate jdbcTemplate;

    public boolean baseDatosDisponible() {
        Integer resultado = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return resultado != null && resultado == 1;
    }

    public List<SerieConfigurada> listarSeriesProduccion() {
        return jdbcTemplate.query("""
            SELECT tipo_documento, serie, ultimo_correlativo, activo
            FROM serie_comprobante
            WHERE ambiente = 'PRODUCCION'
            ORDER BY tipo_documento, serie
            """, (resultSet, rowNumber) -> new SerieConfigurada(
            resultSet.getString("tipo_documento"),
            resultSet.getString("serie"),
            resultSet.getLong("ultimo_correlativo"),
            resultSet.getBoolean("activo")
        ));
    }

    public long contarComprobantesProduccion() {
        Long total = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM comprobante WHERE ambiente = 'PRODUCCION'",
            Long.class
        );
        return total == null ? 0 : total;
    }

    public long contarComprobantesBetaPendientes() {
        Long total = jdbcTemplate.queryForObject("""
            SELECT COUNT(*)
            FROM comprobante
            WHERE ambiente = 'BETA'
              AND estado IN ('PENDIENTE_ENVIO', 'BAJA_PENDIENTE')
            """, Long.class);
        return total == null ? 0 : total;
    }

    public record SerieConfigurada(
        String tipoDocumento,
        String serie,
        long ultimoCorrelativo,
        boolean activa
    ) {
    }
}
