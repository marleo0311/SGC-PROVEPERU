package pe.com.proveperu.sgc.facturacionelectronica.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.NotaElectronicaResponse;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.ResultadoCdr;
import pe.com.proveperu.sgc.facturacionelectronica.application.port.SunatGateway;

class NotaElectronicaServiceTests {
    private final NotaElectronicaPersistenceService persistence = mock(NotaElectronicaPersistenceService.class);
    private final SunatGateway gateway = mock(SunatGateway.class);
    private final CdrParser cdrParser = mock(CdrParser.class);
    private final NotaElectronicaService service = new NotaElectronicaService(persistence, gateway, cdrParser);

    @Test
    void enviaNotaYRegistraSuCdr() {
        byte[] zip = {1, 2}; byte[] cdr = {3, 4};
        var preparada = new NotaElectronicaPersistenceService.NotaPreparada(9L, "20612296911", "nota.zip", zip, null);
        var resultado = new ResultadoCdr("0", "Aceptada", List.of());
        NotaElectronicaResponse esperado = mock(NotaElectronicaResponse.class);
        when(persistence.marcarEnviando(9L)).thenReturn(preparada);
        when(gateway.enviarComprobante("20612296911", "nota.zip", zip)).thenReturn(cdr);
        when(cdrParser.procesar(cdr)).thenReturn(resultado);
        when(persistence.registrarCdr(9L, cdr, resultado)).thenReturn(esperado);

        assertThat(service.enviar(9L)).isSameAs(esperado);
        verify(persistence).registrarCdr(9L, cdr, resultado);
    }
}
