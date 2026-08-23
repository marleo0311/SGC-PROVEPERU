package pe.com.proveperu.sgc.cliente.infrastructure.documento;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import pe.com.proveperu.sgc.cliente.application.exception.LimiteConsultaDocumentoException;
import pe.com.proveperu.sgc.cliente.infrastructure.config.ConsultaDocumentoProperties;

class ConsultaDocumentoRateLimiterTests {

    @Test
    void limitaCadaUsuarioSinAfectarAUsuariosDiferentes() {
        ConsultaDocumentoProperties properties = new ConsultaDocumentoProperties();
        properties.setRateLimitRequests(2);
        properties.setRateLimitWindow(Duration.ofMinutes(1));
        ConsultaDocumentoRateLimiter limiter = new ConsultaDocumentoRateLimiter(
            properties,
            new ConsultaDocumentoMetricas(new SimpleMeterRegistry()),
            Clock.fixed(Instant.parse("2026-08-23T12:00:00Z"), ZoneOffset.UTC)
        );

        limiter.verificar("marco");
        limiter.verificar("marco");

        assertThatThrownBy(() -> limiter.verificar("marco"))
            .isInstanceOf(LimiteConsultaDocumentoException.class);
        assertThatCode(() -> limiter.verificar("otro-usuario")).doesNotThrowAnyException();
    }
}
