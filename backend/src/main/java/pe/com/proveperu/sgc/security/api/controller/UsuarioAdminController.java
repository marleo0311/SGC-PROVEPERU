package pe.com.proveperu.sgc.security.api.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.proveperu.sgc.security.api.dto.PaginaResponse;
import pe.com.proveperu.sgc.security.api.dto.UsuarioActualizarRequest;
import pe.com.proveperu.sgc.security.api.dto.UsuarioCrearRequest;
import pe.com.proveperu.sgc.security.api.dto.UsuarioEstadoRequest;
import pe.com.proveperu.sgc.security.api.dto.UsuarioPasswordRequest;
import pe.com.proveperu.sgc.security.api.dto.UsuarioResponse;
import pe.com.proveperu.sgc.security.application.service.PermisosSeguridad;
import pe.com.proveperu.sgc.security.application.service.UsuarioAdminService;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Validated
public class UsuarioAdminController {

    private final UsuarioAdminService usuarioAdminService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisosSeguridad.USUARIOS_VER + "')")
    public PaginaResponse<UsuarioResponse> listar(
        @RequestParam(defaultValue = "") String buscar,
        @RequestParam(defaultValue = "0") @Min(0) int pagina,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int tamanio
    ) {
        PageRequest pageable = PageRequest.of(
            pagina,
            tamanio,
            Sort.by(Sort.Direction.ASC, "nombreCompleto")
        );
        return usuarioAdminService.listar(buscar, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisosSeguridad.USUARIOS_VER + "')")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return usuarioAdminService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermisosSeguridad.USUARIOS_CREAR + "')")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioCrearRequest request) {
        UsuarioResponse usuario = usuarioAdminService.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/usuarios/" + usuario.id())).body(usuario);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisosSeguridad.USUARIOS_EDITAR + "')")
    public UsuarioResponse actualizar(
        @PathVariable Long id,
        @Valid @RequestBody UsuarioActualizarRequest request
    ) {
        return usuarioAdminService.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('" + PermisosSeguridad.USUARIOS_ESTADO + "')")
    public UsuarioResponse cambiarEstado(
        @PathVariable Long id,
        @Valid @RequestBody UsuarioEstadoRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return usuarioAdminService.cambiarEstado(id, request.estado(), obtenerIdUsuario(jwt));
    }

    @PatchMapping("/{id}/password")
    @PreAuthorize("hasAuthority('" + PermisosSeguridad.USUARIOS_PASSWORD + "')")
    public ResponseEntity<Void> cambiarPassword(
        @PathVariable Long id,
        @Valid @RequestBody UsuarioPasswordRequest request
    ) {
        usuarioAdminService.cambiarPassword(id, request.password());
        return ResponseEntity.noContent().build();
    }

    private Long obtenerIdUsuario(Jwt jwt) {
        Number idUsuario = jwt.getClaim("userId");
        return idUsuario.longValue();
    }
}
