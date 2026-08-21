package pe.com.proveperu.sgc.config;

import java.time.Duration;
import java.util.Base64;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.security.jwt")
@Getter
@Setter
public class JwtProperties {

    private String secret = "";
    private String issuer = "sgc-proveperu";
    private Duration expiration = Duration.ofHours(2);

    public byte[] decodedSecret() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                "JWT_SECRET es obligatorio y debe contener un secreto Base64 de al menos 32 bytes"
            );
        }

        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(secret.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET debe tener formato Base64 válido", exception);
        }

        if (decoded.length < 32) {
            throw new IllegalStateException("JWT_SECRET debe contener al menos 32 bytes");
        }

        return decoded;
    }
}
