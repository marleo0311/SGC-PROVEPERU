package pe.com.proveperu.sgc.security.api.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.proveperu.sgc.security.api.dto.RolCrearRequest;
import pe.com.proveperu.sgc.security.api.dto.RolDetalleResponse;
import pe.com.proveperu.sgc.security.api.dto.RolPermisosRequest;
import pe.com.proveperu.sgc.security.api.dto.RolResumenResponse;
import pe.com.proveperu.sgc.security.application.service.PermisosSeguridad;
import pe.com.proveperu.sgc.security.application.service.RolAdminService;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RolAdminController {

    private final RolAdminService rolAdminService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisosSeguridad.ROLES_VER + "')")
    public List<RolResumenResponse> listar() {
        return rolAdminService.listar();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisosSeguridad.ROLES_VER + "')")
    public RolDetalleResponse obtener(@PathVariable Long id) {
        return rolAdminService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermisosSeguridad.ROLES_CREAR + "')")
    public ResponseEntity<RolDetalleResponse> crear(@Valid @RequestBody RolCrearRequest request) {
        RolDetalleResponse rol = rolAdminService.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/roles/" + rol.id())).body(rol);
    }

    @PatchMapping("/{id}/permisos")
    @PreAuthorize("hasAuthority('" + PermisosSeguridad.ROLES_PERMISOS + "')")
    public RolDetalleResponse actualizarPermisos(
        @PathVariable Long id,
        @Valid @RequestBody RolPermisosRequest request
    ) {
        return rolAdminService.actualizarPermisos(id, request.idsPermisos());
    }
}
