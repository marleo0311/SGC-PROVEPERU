package pe.com.proveperu.sgc.security.api.controller;

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

@RestController
@RequestMapping("/api/v1/permisos")
@RequiredArgsConstructor
public class PermisoController {

    private final PermisoConsultaService permisoConsultaService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisosSeguridad.PERMISOS_VER + "')")
    public List<PermisoResponse> listar(@RequestParam(required = false) String modulo) {
        return permisoConsultaService.listar(modulo);
    }
}
