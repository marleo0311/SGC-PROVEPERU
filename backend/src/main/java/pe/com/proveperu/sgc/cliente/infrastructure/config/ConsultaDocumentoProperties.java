package pe.com.proveperu.sgc.cliente.infrastructure.config;

import java.net.URI;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import pe.com.proveperu.sgc.cliente.domain.model.TipoDocumentoCliente;

@Component
@ConfigurationProperties(prefix = "app.consulta-documento")
@Getter
@Setter
public class ConsultaDocumentoProperties {

    private boolean enabled;
    private boolean dniEnabled;
    private URI baseUrl = URI.create("https://api.decolecta.com/v1");
    private String token = "";
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(10);
    private Duration cacheTtl = Duration.ofHours(24);
    private Duration negativeCacheTtl = Duration.ofMinutes(5);
    private int cacheMaxEntries = 5_000;
    private int rateLimitRequests = 10;
    private Duration rateLimitWindow = Duration.ofMinutes(1);
    private int rateLimitMaxUsers = 10_000;
    private int retryMaxAttempts = 3;
    private Duration retryDelay = Duration.ofMillis(250);
    private int circuitBreakerFailures = 5;
    private Duration circuitBreakerOpenDuration = Duration.ofSeconds(30);

    public boolean disponible(TipoDocumentoCliente tipoDocumento) {
        return enabled
            && token != null
            && !token.isBlank()
            && (tipoDocumento != TipoDocumentoCliente.DNI || dniEnabled);
    }
}
