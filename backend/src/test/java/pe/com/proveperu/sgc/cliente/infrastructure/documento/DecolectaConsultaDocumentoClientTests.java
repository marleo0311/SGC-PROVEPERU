package pe.com.proveperu.sgc.cliente.infrastructure.documento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import com.sun.net.httpserver.HttpServer;
import pe.com.proveperu.sgc.cliente.domain.model.TipoDocumentoCliente;
import pe.com.proveperu.sgc.cliente.infrastructure.config.ConsultaDocumentoProperties;

class DecolectaConsultaDocumentoClientTests {

    private final DecolectaConsultaDocumentoClient client = new DecolectaConsultaDocumentoClient(
        new ConsultaDocumentoProperties(),
        new SimpleMeterRegistry()
    );

    @Test
    void interpretaLaRespuestaDeRuc() {
        var datos = client.parsear(TipoDocumentoCliente.RUC, "20601030013", """
            {
              "razon_social": "REXTIE S.A.C.",
              "numero_documento": "20601030013",
              "direccion": "AV. JOSE GALVEZ BARRENECHEA 566",
              "estado": "ACTIVO",
              "condicion": "HABIDO"
            }
            """);

        assertThat(datos.razonSocial()).isEqualTo("REXTIE S.A.C.");
        assertThat(datos.direccion()).isEqualTo("AV. JOSE GALVEZ BARRENECHEA 566");
        assertThat(datos.estadoContribuyente()).isEqualTo("ACTIVO");
        assertThat(datos.condicionDomicilio()).isEqualTo("HABIDO");
    }

    @Test
    void interpretaNombresYApellidosDeDni() {
        var datos = client.parsear(TipoDocumentoCliente.DNI, "46027897", """
            {
              "first_name": "ERACLEO JUAN",
              "first_last_name": "HUAMANI",
              "second_last_name": "MENDOZA",
              "document_number": "46027897"
            }
            """);

        assertThat(datos.nombres()).isEqualTo("ERACLEO JUAN");
        assertThat(datos.apellidos()).isEqualTo("HUAMANI MENDOZA");
        assertThat(datos.nombreMostrar()).isEqualTo("ERACLEO JUAN HUAMANI MENDOZA");
    }

    @Test
    void conservaCompatibilidadConLaRespuestaAnterior() {
        var datos = client.parsear(TipoDocumentoCliente.RUC, "20601030013", """
            {
              "razonSocial": "REXTIE S.A.C.",
              "estado": "ACTIVO",
              "condicion": "HABIDO"
            }
            """);

        assertThat(datos.razonSocial()).isEqualTo("REXTIE S.A.C.");
    }

    @Test
    void reintentaErroresTemporalesYConservaElResultadoEnCache() throws Exception {
        AtomicInteger solicitudes = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/sunat/ruc", exchange -> {
            int numeroSolicitud = solicitudes.incrementAndGet();
            int estado = numeroSolicitud < 3 ? 500 : 200;
            byte[] contenido = (estado == 200
                ? "{\"razon_social\":\"REXTIE S.A.C.\",\"estado\":\"ACTIVO\",\"condicion\":\"HABIDO\"}"
                : "{\"error\":\"temporal\"}").getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(estado, contenido.length);
            exchange.getResponseBody().write(contenido);
            exchange.close();
        });
        server.start();
        try {
            ConsultaDocumentoProperties properties = propiedadesHabilitadas(server);
            properties.setRetryMaxAttempts(3);
            properties.setRetryDelay(Duration.ZERO);
            DecolectaConsultaDocumentoClient resilientClient = new DecolectaConsultaDocumentoClient(
                properties,
                new SimpleMeterRegistry()
            );

            var primera = resilientClient.consultar(TipoDocumentoCliente.RUC, "20601030013");
            var segunda = resilientClient.consultar(TipoDocumentoCliente.RUC, "20601030013");

            assertThat(primera).isPresent();
            assertThat(segunda).isPresent();
            assertThat(solicitudes).hasValue(3);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void cacheaTemporalmenteLosDocumentosNoEncontrados() throws Exception {
        AtomicInteger solicitudes = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/sunat/ruc", exchange -> {
            solicitudes.incrementAndGet();
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        try {
            DecolectaConsultaDocumentoClient resilientClient = new DecolectaConsultaDocumentoClient(
                propiedadesHabilitadas(server),
                new SimpleMeterRegistry()
            );

            assertThat(resilientClient.consultar(TipoDocumentoCliente.RUC, "20601030013")).isEmpty();
            assertThat(resilientClient.consultar(TipoDocumentoCliente.RUC, "20601030013")).isEmpty();
            assertThat(solicitudes).hasValue(1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void abreElCircuitoDespuesDeFallosConsecutivos() throws Exception {
        AtomicInteger solicitudes = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/sunat/ruc", exchange -> {
            solicitudes.incrementAndGet();
            byte[] contenido = "{\"error\":\"temporal\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(500, contenido.length);
            exchange.getResponseBody().write(contenido);
            exchange.close();
        });
        server.start();
        try {
            ConsultaDocumentoProperties properties = propiedadesHabilitadas(server);
            properties.setRetryMaxAttempts(1);
            properties.setCircuitBreakerFailures(2);
            properties.setCircuitBreakerOpenDuration(Duration.ofMinutes(1));
            DecolectaConsultaDocumentoClient resilientClient = new DecolectaConsultaDocumentoClient(
                properties,
                new SimpleMeterRegistry()
            );

            assertThatThrownBy(() -> resilientClient.consultar(TipoDocumentoCliente.RUC, "20601030013"))
                .isInstanceOf(IntegracionDocumentoException.class);
            assertThatThrownBy(() -> resilientClient.consultar(TipoDocumentoCliente.RUC, "20601030013"))
                .isInstanceOf(IntegracionDocumentoException.class);
            assertThatThrownBy(() -> resilientClient.consultar(TipoDocumentoCliente.RUC, "20601030013"))
                .isInstanceOf(IntegracionDocumentoException.class)
                .hasMessageContaining("temporalmente protegido");
            assertThat(solicitudes).hasValue(2);
        } finally {
            server.stop(0);
        }
    }

    private ConsultaDocumentoProperties propiedadesHabilitadas(HttpServer server) {
        ConsultaDocumentoProperties properties = new ConsultaDocumentoProperties();
        properties.setEnabled(true);
        properties.setToken("token-prueba");
        properties.setBaseUrl(URI.create(
            "http://127.0.0.1:" + server.getAddress().getPort() + "/v1"
        ));
        properties.setConnectTimeout(Duration.ofSeconds(1));
        properties.setReadTimeout(Duration.ofSeconds(1));
        return properties;
    }
}
