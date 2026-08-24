package pe.com.proveperu.sgc.facturacionelectronica.api.dto;

import pe.com.proveperu.sgc.facturacionelectronica.domain.model.AmbienteSunat;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.config.SunatProperties;

public record ConfiguracionSunatResponse(
    boolean habilitado,
    AmbienteSunat ambiente,
    boolean produccionHabilitada,
    boolean certificadoConfigurado,
    boolean credencialesConfiguradas,
    boolean resumenDiarioAutomatico,
    boolean resumenDiarioAutoEnviar,
    String endpoint,
    String advertencia
) {
    public static ConfiguracionSunatResponse from(SunatProperties properties) {
        boolean certificate = properties.getCertificadoRuta() != null
            && !properties.getCertificadoRuta().isBlank()
            && properties.getCertificadoClave() != null
            && !properties.getCertificadoClave().isBlank();
        boolean credentials = properties.getUsuarioSol() != null
            && !properties.getUsuarioSol().isBlank()
            && properties.getClaveSol() != null
            && !properties.getClaveSol().isBlank();
        String warning = properties.getAmbiente() == AmbienteSunat.BETA
            ? "BETA valida estructuras de prueba; no acredita comprobantes reales"
            : "Producción transmite comprobantes tributarios reales";
        return new ConfiguracionSunatResponse(
            properties.isEnabled(),
            properties.getAmbiente(),
            properties.isProductionEnabled(),
            certificate,
            credentials,
            properties.isResumenDiarioAutomaticoEnabled(),
            properties.isResumenDiarioAutoEnviar(),
            properties.endpoint().toString(),
            warning
        );
    }
}
