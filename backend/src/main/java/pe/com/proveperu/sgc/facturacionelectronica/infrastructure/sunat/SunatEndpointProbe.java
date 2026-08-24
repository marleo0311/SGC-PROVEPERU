package pe.com.proveperu.sgc.facturacionelectronica.infrastructure.sunat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.springframework.stereotype.Component;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.config.SunatProperties;

@Component
public class SunatEndpointProbe {

    private final SunatProperties properties;

    public SunatEndpointProbe(SunatProperties properties) {
        this.properties = properties;
    }

    public Resultado verificar(URI endpoint) {
        URI wsdl = URI.create(endpoint + (endpoint.getQuery() == null ? "?wsdl" : "&wsdl"));
        HttpClient client = HttpClient.newBuilder()
            .connectTimeout(properties.getConnectTimeout())
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
        HttpRequest request = HttpRequest.newBuilder(wsdl)
            .GET()
            .timeout(limitar(properties.getReadTimeout()))
            .header("User-Agent", "SGC-PROVEPERU-Diagnostico/1.0")
            .build();
        try {
            HttpResponse<Void> response = client.send(
                request,
                HttpResponse.BodyHandlers.discarding()
            );
            return new Resultado(true, response.statusCode(), "SUNAT respondió por HTTPS");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return new Resultado(false, null, "La comprobación de conectividad fue interrumpida");
        } catch (IOException | IllegalArgumentException exception) {
            return new Resultado(false, null, "No se pudo establecer conexión HTTPS con SUNAT");
        }
    }

    private Duration limitar(Duration configured) {
        Duration duration = configured == null ? Duration.ofSeconds(15) : configured;
        return duration.compareTo(Duration.ofSeconds(20)) > 0
            ? Duration.ofSeconds(20)
            : duration;
    }

    public record Resultado(boolean alcanzable, Integer codigoHttp, String detalle) {
    }
}
