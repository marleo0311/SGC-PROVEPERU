package pe.com.proveperu.sgc.facturacionelectronica.infrastructure.sunat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.config.SunatProperties;

class SunatSoapClientTests {

    private final SunatSoapClient client = new SunatSoapClient(new SunatProperties());

    @Test
    void extraeCdrBase64DeRespuestaSoap() {
        byte[] cdr = "cdr-prueba".getBytes(StandardCharsets.UTF_8);
        String soap = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
              <soapenv:Body><sendBillResponse><applicationResponse>%s</applicationResponse></sendBillResponse></soapenv:Body>
            </soapenv:Envelope>
            """.formatted(Base64.getEncoder().encodeToString(cdr));

        assertThat(client.extraerCdr(soap.getBytes(StandardCharsets.UTF_8))).isEqualTo(cdr);
    }

    @Test
    void convierteSoapFaultEnErrorControlado() {
        String soap = """
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
              <soapenv:Body><soapenv:Fault><faultcode>soapenv:Client.1033</faultcode><faultstring>Serie inválida</faultstring></soapenv:Fault></soapenv:Body>
            </soapenv:Envelope>
            """;

        assertThatThrownBy(() -> client.extraerCdr(soap.getBytes(StandardCharsets.UTF_8)))
            .isInstanceOfSatisfying(RechazoSunatException.class, exception -> {
                assertThat(exception.getCodigo()).isEqualTo("1033");
                assertThat(exception).hasMessageContaining("Serie inválida");
            });
    }
}
