package pe.com.proveperu.sgc.comprobante.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.comprobante.domain.model.TipoNumeracionComprobante;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.AmbienteSunat;
import pe.com.proveperu.sgc.security.application.exception.OperacionNoPermitidaException;

@SpringBootTest(properties = {
    "app.security.jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
    "app.sunat.enabled=false",
    "app.sunat.ambiente=BETA"
})
@Transactional
class CorrelativoComprobanteIntegrationTests {

    @Autowired
    private CorrelativoComprobanteService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void reservaCorrelativosIndependientesParaBetaYProduccion() {
        Map<String, Object> sede = jdbcTemplate.queryForMap("""
            SELECT id_empresa, id_sede
            FROM sede
            WHERE estado = 'ACTIVO'
            ORDER BY id_sede
            LIMIT 1
            """);
        long idEmpresa = ((Number) sede.get("id_empresa")).longValue();
        long idSede = ((Number) sede.get("id_sede")).longValue();
        long betaAntes = ultimo(
            idEmpresa, idSede, AmbienteSunat.BETA, TipoNumeracionComprobante.BOLETA
        );
        long produccionAntes = ultimo(
            idEmpresa, idSede, AmbienteSunat.PRODUCCION, TipoNumeracionComprobante.BOLETA
        );

        var beta = service.siguiente(
            idEmpresa, idSede, AmbienteSunat.BETA, TipoNumeracionComprobante.BOLETA
        );
        var produccion = service.siguiente(
            idEmpresa, idSede, AmbienteSunat.PRODUCCION,
            TipoNumeracionComprobante.BOLETA
        );

        assertThat(beta.serie()).isEqualTo("B001");
        assertThat(beta.correlativo()).isEqualTo(betaAntes + 1);
        assertThat(produccion.serie()).isEqualTo("B001");
        assertThat(produccion.correlativo()).isEqualTo(produccionAntes + 1);
    }

    @Test
    void rechazaUnaSedeSinSerieActiva() {
        assertThatThrownBy(() -> service.siguiente(
            Long.MAX_VALUE,
            Long.MAX_VALUE,
            AmbienteSunat.PRODUCCION,
            TipoNumeracionComprobante.FACTURA
        ))
            .isInstanceOf(OperacionNoPermitidaException.class)
            .hasMessageContaining("serie activa");
    }

    private long ultimo(
        long idEmpresa,
        long idSede,
        AmbienteSunat ambiente,
        TipoNumeracionComprobante tipo
    ) {
        Long valor = jdbcTemplate.queryForObject("""
            SELECT ultimo_correlativo
            FROM serie_comprobante
            WHERE id_empresa = ?
              AND id_sede = ?
              AND ambiente = ?
              AND tipo_documento = ?
              AND activo = TRUE
            """, Long.class, idEmpresa, idSede, ambiente.name(), tipo.name());
        return valor == null ? 0 : valor;
    }
}
