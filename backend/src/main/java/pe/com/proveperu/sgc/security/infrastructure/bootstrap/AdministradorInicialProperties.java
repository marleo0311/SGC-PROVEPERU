package pe.com.proveperu.sgc.security.infrastructure.bootstrap;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.bootstrap.admin")
@Getter
@Setter
public class AdministradorInicialProperties {

    private boolean enabled;
    private boolean resetPassword;
    private String login;
    private String password;
    private String nombreCompleto = "Administrador del sistema";
}
