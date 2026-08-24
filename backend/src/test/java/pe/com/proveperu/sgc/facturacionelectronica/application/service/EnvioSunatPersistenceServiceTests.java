package pe.com.proveperu.sgc.facturacionelectronica.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import pe.com.proveperu.sgc.comprobante.domain.model.Comprobante;
import pe.com.proveperu.sgc.comprobante.infrastructure.persistence.ComprobanteRepository;
import pe.com.proveperu.sgc.configuracion.infrastructure.persistence.EmpresaRepository;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.AmbienteSunat;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.EnvioSunat;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.EstadoEnvioSunat;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.config.SunatProperties;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.persistence.EnvioSunatRepository;
import pe.com.proveperu.sgc.security.application.exception.OperacionNoPermitidaException;
import pe.com.proveperu.sgc.venta.domain.model.TipoComprobanteVenta;

class EnvioSunatPersistenceServiceTests {

    @Test
    void dirigeBoletaDeProduccionAlFlujoDeResumenDiario() {
        Comprobante comprobante = new Comprobante();
        comprobante.setTipo(TipoComprobanteVenta.BOLETA);
        EnvioSunat envio = new EnvioSunat();
        envio.setComprobante(comprobante);
        envio.setEstado(EstadoEnvioSunat.GENERADO);

        EnvioSunatRepository envioRepository = mock(EnvioSunatRepository.class);
        when(envioRepository.findForUpdateByComprobanteId(91L))
            .thenReturn(Optional.of(envio));
        SunatProperties properties = new SunatProperties();
        properties.setAmbiente(AmbienteSunat.PRODUCCION);
        EnvioSunatPersistenceService service = new EnvioSunatPersistenceService(
            mock(ComprobanteRepository.class),
            mock(EmpresaRepository.class),
            envioRepository,
            mock(DocumentoElectronicoService.class),
            properties
        );

        assertThatThrownBy(() -> service.marcarEnviando(91L))
            .isInstanceOf(OperacionNoPermitidaException.class)
            .hasMessageContaining("Resúmenes SUNAT");
        verify(envioRepository, never()).saveAndFlush(envio);
    }
}
