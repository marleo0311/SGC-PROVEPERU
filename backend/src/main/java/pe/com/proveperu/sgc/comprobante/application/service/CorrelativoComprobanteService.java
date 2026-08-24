package pe.com.proveperu.sgc.comprobante.application.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import pe.com.proveperu.sgc.comprobante.domain.model.TipoNumeracionComprobante;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.AmbienteSunat;
import pe.com.proveperu.sgc.security.application.exception.OperacionNoPermitidaException;

@Service
@RequiredArgsConstructor
public class CorrelativoComprobanteService {

    private static final long MAXIMO_CORRELATIVO = 99_999_999L;

    private final JdbcTemplate jdbcTemplate;

    public NumeracionComprobante siguiente(
        Long idEmpresa,
        Long idSede,
        AmbienteSunat ambiente,
        TipoNumeracionComprobante tipoDocumento
    ) {
        List<NumeracionComprobante> resultados = jdbcTemplate.query("""
            UPDATE serie_comprobante
            SET ultimo_correlativo = ultimo_correlativo + 1,
                fecha_actualizacion = CURRENT_TIMESTAMP
            WHERE id_empresa = ?
              AND id_sede = ?
              AND ambiente = ?
              AND tipo_documento = ?
              AND activo = TRUE
              AND ultimo_correlativo < ?
            RETURNING serie, ultimo_correlativo
            """,
            (resultSet, rowNumber) -> new NumeracionComprobante(
                resultSet.getString("serie"),
                resultSet.getLong("ultimo_correlativo")
            ),
            idEmpresa,
            idSede,
            ambiente.name(),
            tipoDocumento.name(),
            MAXIMO_CORRELATIVO
        );

        if (resultados.isEmpty()) {
            throw new OperacionNoPermitidaException(
                "No existe una serie activa o no quedan correlativos disponibles para "
                    + tipoDocumento + " en " + ambiente
            );
        }
        if (resultados.size() > 1) {
            throw new IllegalStateException(
                "Existe más de una serie activa para la sede, ambiente y tipo documental"
            );
        }
        return resultados.getFirst();
    }

    public record NumeracionComprobante(String serie, long correlativo) {

        public String numero() {
            return "%08d".formatted(correlativo);
        }
    }
}
