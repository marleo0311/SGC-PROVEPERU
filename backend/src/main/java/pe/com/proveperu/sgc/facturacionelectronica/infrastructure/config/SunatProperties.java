package pe.com.proveperu.sgc.facturacionelectronica.infrastructure.config;

import java.net.URI;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.AmbienteSunat;

@Component
@ConfigurationProperties(prefix = "app.sunat")
@Getter
@Setter
public class SunatProperties {

    private boolean enabled;
    private boolean productionEnabled;
    private AmbienteSunat ambiente = AmbienteSunat.BETA;
    private String usuarioSol = "MODDATOS";
    private String claveSol = "MODDATOS";
    private String certificadoRuta = "";
    private String certificadoClave = "";
    private URI endpointBeta = URI.create(
        "https://e-beta.sunat.gob.pe/ol-ti-itcpfegem-beta/billService"
    );
    private URI endpointProduccion = URI.create(
        "https://e-factura.sunat.gob.pe/ol-ti-itcpfegem/billService"
    );
    private Duration connectTimeout = Duration.ofSeconds(15);
    private Duration readTimeout = Duration.ofSeconds(45);
    private boolean resumenDiarioAutomaticoEnabled;
    private boolean resumenDiarioAutoEnviar;
    private String resumenDiarioCron = "0 15 2 * * *";

    public URI endpoint() {
        return ambiente == AmbienteSunat.PRODUCCION
            ? endpointProduccion
            : endpointBeta;
    }

    public String username(String ruc) {
        String usuario = usuarioSol == null ? "" : usuarioSol.strip();
        return usuario.startsWith(ruc) ? usuario : ruc + usuario;
    }
}
