package pe.com.proveperu.sgc.facturacionelectronica.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.proveperu.sgc.config.OpenApiConfig;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.DiagnosticoSunatResponse;
import pe.com.proveperu.sgc.facturacionelectronica.application.service.DiagnosticoSunatService;
import pe.com.proveperu.sgc.venta.application.service.PermisosVenta;

@RestController
@RequestMapping("/api/v1/sunat")
@RequiredArgsConstructor
@Tag(
    name = "Diagnóstico SUNAT",
    description = "Validaciones de solo lectura previas a habilitar la emisión real"
)
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class DiagnosticoSunatController {

    private final DiagnosticoSunatService service;

    @GetMapping("/diagnostico-produccion")
    @PreAuthorize("hasAuthority('" + PermisosVenta.SUNAT_ENVIAR + "')")
    @Operation(
        summary = "Diagnosticar la preparación para producción sin consumir correlativos"
    )
    public DiagnosticoSunatResponse diagnosticarProduccion() {
        return service.diagnosticarProduccion();
    }
}
