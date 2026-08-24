package pe.com.proveperu.sgc.comprobante.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import pe.com.proveperu.sgc.comprobante.infrastructure.persistence.ComprobanteRepository;
import pe.com.proveperu.sgc.configuracion.infrastructure.persistence.EmpresaRepository;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.AmbienteSunat;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.config.SunatProperties;
import pe.com.proveperu.sgc.security.application.exception.OperacionNoPermitidaException;
import pe.com.proveperu.sgc.venta.domain.model.TipoComprobanteVenta;
import pe.com.proveperu.sgc.venta.domain.model.Venta;

class ComprobanteServiceTests {

    private final ComprobanteService service = new ComprobanteService(
        mock(ComprobanteRepository.class),
        mock(EmpresaRepository.class),
        mock(CorrelativoComprobanteService.class),
        new SunatProperties()
    );

    @Test
    void exigeClienteIdentificadoEnBoletaMayorASetecientosSoles() {
        Venta venta = venta(TipoComprobanteVenta.BOLETA, "700.01");

        assertThatThrownBy(() -> service.validarEmision(venta))
            .isInstanceOf(OperacionNoPermitidaException.class)
            .hasMessageContaining("DNI o RUC");
    }

    @Test
    void permiteClienteOcasionalHastaSetecientosSoles() {
        Venta venta = venta(TipoComprobanteVenta.BOLETA, "700.00");

        assertThatCode(() -> service.validarEmision(venta)).doesNotThrowAnyException();
    }

    @Test
    void bloqueaLaNumeracionRealMientrasProduccionNoEsteHabilitada() {
        ComprobanteRepository repository = mock(ComprobanteRepository.class);
        when(repository.findByVentaId(null)).thenReturn(Optional.empty());
        SunatProperties properties = new SunatProperties();
        properties.setAmbiente(AmbienteSunat.PRODUCCION);
        properties.setProductionEnabled(false);
        ComprobanteService productionService = new ComprobanteService(
            repository,
            mock(EmpresaRepository.class),
            mock(CorrelativoComprobanteService.class),
            properties
        );

        assertThatThrownBy(() -> productionService.emitirParaVenta(
            venta(TipoComprobanteVenta.BOLETA, "100.00")
        ))
            .isInstanceOf(OperacionNoPermitidaException.class)
            .hasMessageContaining("SUNAT_PRODUCTION_ENABLED=false");
    }

    private Venta venta(TipoComprobanteVenta tipo, String total) {
        Venta venta = new Venta();
        venta.setTipoComprobante(tipo);
        venta.setTotal(new BigDecimal(total));
        return venta;
    }
}
