package pe.com.proveperu.sgc.facturacionelectronica.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.ResumenDiarioSunatResponse;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.EstadoTicketSunat;
import pe.com.proveperu.sgc.facturacionelectronica.application.port.SunatGateway;

class ResumenDiarioSunatServiceTests {

    private final ResumenDiarioSunatPersistenceService persistence =
        mock(ResumenDiarioSunatPersistenceService.class);
    private final SunatGateway gateway = mock(SunatGateway.class);
    private final CdrParser cdrParser = mock(CdrParser.class);
    private final ResumenDiarioSunatService service =
        new ResumenDiarioSunatService(persistence, gateway, cdrParser);

    @Test
    void guardaElTicketDevueltoPorSendSummary() {
        byte[] zip = {1, 2, 3};
        var preparado = new ResumenDiarioSunatPersistenceService.ResumenPreparado(
            8L,
            "20612296911",
            "20612296911-RC-20260822-1.zip",
            zip,
            null
        );
        ResumenDiarioSunatResponse esperado = mock(ResumenDiarioSunatResponse.class);
        when(persistence.marcarEnviando(8L)).thenReturn(preparado);
        when(gateway.enviarResumen("20612296911", preparado.nombreZip(), zip))
            .thenReturn("202608231234567");
        when(persistence.registrarTicket(8L, "202608231234567")).thenReturn(esperado);

        assertThat(service.enviar(8L)).isSameAs(esperado);
        verify(persistence).registrarTicket(8L, "202608231234567");
    }

    @Test
    void conservaElTicketCuandoSunatContinuaProcesando() {
        var consulta = new ResumenDiarioSunatPersistenceService.ConsultaTicket(
            8L,
            "20612296911",
            "202608231234567",
            null
        );
        ResumenDiarioSunatResponse esperado = mock(ResumenDiarioSunatResponse.class);
        when(persistence.marcarConsultando(8L)).thenReturn(consulta);
        when(gateway.consultarTicket("20612296911", consulta.ticket()))
            .thenReturn(new EstadoTicketSunat("98", "En proceso", null));
        when(persistence.registrarProcesando(8L, "98", "En proceso"))
            .thenReturn(esperado);

        assertThat(service.consultarEstado(8L)).isSameAs(esperado);
        verify(persistence).registrarProcesando(8L, "98", "En proceso");
    }
}
