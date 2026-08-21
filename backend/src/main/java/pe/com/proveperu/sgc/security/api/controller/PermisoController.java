package pe.com.proveperu.sgc.security.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.proveperu.sgc.security.api.dto.PermisoResponse;
import pe.com.proveperu.sgc.security.application.service.PermisoConsultaService;
import pe.com.proveperu.sgc.security.application.service.PermisosSeguridad;
import pe.com.proveperu.sgc.config.OpenApiConfig;

@RestController
@RequestMapping("/api/v1/permisos")
@RequiredArgsConstructor
@Tag(name = "Permisos", description = "Consulta de permisos disponibles")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class PermisoController {

    private final PermisoConsultaService permisoConsultaService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisosSeguridad.PERMISOS_VER + "')")
    @Operation(summary = "Listar permisos", description = "Permite filtrar los permisos por módulo")
    public List<PermisoResponse> listar(@RequestParam(required = false) String modulo) {
        return permisoConsultaService.listar(modulo);
    }
}
