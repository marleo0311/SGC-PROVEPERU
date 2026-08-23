package pe.com.proveperu.sgc.cliente.infrastructure.documento;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pe.com.proveperu.sgc.cliente.application.exception.LimiteConsultaDocumentoException;
import pe.com.proveperu.sgc.cliente.infrastructure.config.ConsultaDocumentoProperties;

@Component
public class ConsultaDocumentoRateLimiter {

    private final ConsultaDocumentoProperties properties;
    private final ConsultaDocumentoMetricas metricas;
    private final Clock clock;
    private final Map<String, Ventana> ventanas = new ConcurrentHashMap<>();

    @Autowired
    public ConsultaDocumentoRateLimiter(
        ConsultaDocumentoProperties properties,
        ConsultaDocumentoMetricas metricas
    ) {
        this(properties, metricas, Clock.systemUTC());
    }

    ConsultaDocumentoRateLimiter(
        ConsultaDocumentoProperties properties,
        ConsultaDocumentoMetricas metricas,
        Clock clock
    ) {
        this.properties = properties;
        this.metricas = metricas;
        this.clock = clock;
    }

    public void verificar(String usuario) {
        if (properties.getRateLimitRequests() <= 0) {
            return;
        }
        Instant ahora = clock.instant();
        String clave = usuario == null || usuario.isBlank() ? "desconocido" : usuario;
        Ventana ventana = ventanas.computeIfAbsent(clave, ignored -> new Ventana(ahora));
        synchronized (ventana) {
            if (!ahora.isBefore(ventana.inicio.plus(properties.getRateLimitWindow()))) {
                ventana.inicio = ahora;
                ventana.solicitudes = 0;
            }
            if (ventana.solicitudes >= properties.getRateLimitRequests()) {
                metricas.registrarLimiteExcedido();
                throw new LimiteConsultaDocumentoException(
                    "Alcanzaste el límite temporal de consultas de documentos; espera un momento e inténtalo nuevamente"
                );
            }
            ventana.solicitudes++;
        }
        limpiarVentanasAntiguas(ahora);
    }

    private void limpiarVentanasAntiguas(Instant ahora) {
        if (ventanas.size() <= properties.getRateLimitMaxUsers()) {
            return;
        }
        Instant limite = ahora.minus(properties.getRateLimitWindow().multipliedBy(2));
        ventanas.entrySet().removeIf(entry -> entry.getValue().inicio.isBefore(limite));
    }

    private static final class Ventana {
        private Instant inicio;
        private int solicitudes;

        private Ventana(Instant inicio) {
            this.inicio = inicio;
        }
    }
}
