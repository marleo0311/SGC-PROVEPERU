package pe.com.proveperu.sgc.security.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.proveperu.sgc.security.api.dto.LoginRequest;
import pe.com.proveperu.sgc.security.api.dto.LoginResponse;
import pe.com.proveperu.sgc.security.api.dto.UsuarioSesionResponse;
import pe.com.proveperu.sgc.security.application.service.AutenticacionService;
import pe.com.proveperu.sgc.config.OpenApiConfig;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Inicio de sesión y consulta del usuario autenticado")
public class AuthController {

    private final AutenticacionService autenticacionService;

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Valida las credenciales y devuelve un token JWT")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return autenticacionService.iniciarSesion(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Consultar mi sesión")
    @SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
    public UsuarioSesionResponse me(@AuthenticationPrincipal Jwt jwt) {
        return autenticacionService.obtenerUsuarioActual(jwt.getSubject());
    }
}
