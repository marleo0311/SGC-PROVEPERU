package pe.com.proveperu.sgc.security.api.controller;

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

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AutenticacionService autenticacionService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return autenticacionService.iniciarSesion(request);
    }

    @GetMapping("/me")
    public UsuarioSesionResponse me(@AuthenticationPrincipal Jwt jwt) {
        return autenticacionService.obtenerUsuarioActual(jwt.getSubject());
    }
}
