package pe.com.proveperu.sgc.facturacionelectronica.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.ComunicacionBajaResponse;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.EstadoTicketSunat;
import pe.com.proveperu.sgc.facturacionelectronica.application.port.SunatGateway;

class ComunicacionBajaServiceTests {
    private final ComunicacionBajaPersistenceService persistence = mock(ComunicacionBajaPersistenceService.class);
    private final SunatGateway gateway = mock(SunatGateway.class);
    private final CdrParser cdrParser = mock(CdrParser.class);
    private final ComunicacionBajaService service = new ComunicacionBajaService(persistence, gateway, cdrParser);

    @Test
    void enviaComunicacionYConservaTicket() {
        byte[] zip = {1, 2, 3};
        var preparada = new ComunicacionBajaPersistenceService.BajaPreparada(4L, "20612296911", "baja.zip", zip, null);
        ComunicacionBajaResponse esperado = mock(ComunicacionBajaResponse.class);
        when(persistence.marcarEnviando(4L)).thenReturn(preparada);
        when(gateway.enviarResumen("20612296911", "baja.zip", zip)).thenReturn("123456789");
        when(persistence.registrarTicket(4L, "123456789")).thenReturn(esperado);

        assertThat(service.enviar(4L)).isSameAs(esperado);
        verify(persistence).registrarTicket(4L, "123456789");
    }

    @Test
    void mantieneBajaEnProcesoCuandoSunatDevuelve98() {
        var consulta = new ComunicacionBajaPersistenceService.ConsultaBaja(4L, "20612296911", "123456789", null);
        ComunicacionBajaResponse esperado = mock(ComunicacionBajaResponse.class);
        when(persistence.marcarConsultando(4L)).thenReturn(consulta);
        when(gateway.consultarTicket("20612296911", "123456789")).thenReturn(new EstadoTicketSunat("98", "En proceso", null));
        when(persistence.registrarProcesando(4L, "98", "En proceso")).thenReturn(esperado);

        assertThat(service.consultar(4L)).isSameAs(esperado);
    }
}
