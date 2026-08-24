package pe.com.proveperu.sgc.comprobante.application.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import pe.com.proveperu.sgc.comprobante.infrastructure.persistence.ComprobanteRepository;
import pe.com.proveperu.sgc.configuracion.infrastructure.persistence.EmpresaRepository;
import pe.com.proveperu.sgc.security.application.exception.OperacionNoPermitidaException;
import pe.com.proveperu.sgc.venta.domain.model.TipoComprobanteVenta;
import pe.com.proveperu.sgc.venta.domain.model.Venta;

class ComprobanteServiceTests {

    private final ComprobanteService service = new ComprobanteService(
        mock(ComprobanteRepository.class),
        mock(EmpresaRepository.class)
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

    private Venta venta(TipoComprobanteVenta tipo, String total) {
        Venta venta = new Venta();
        venta.setTipoComprobante(tipo);
        venta.setTotal(new BigDecimal(total));
        return venta;
    }
}
