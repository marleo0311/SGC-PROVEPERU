package pe.com.proveperu.sgc.security.application.service;

import java.time.Instant;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.security.api.dto.LoginRequest;
import pe.com.proveperu.sgc.security.api.dto.LoginResponse;
import pe.com.proveperu.sgc.security.api.dto.UsuarioSesionResponse;
import pe.com.proveperu.sgc.security.application.exception.CredencialesInvalidasException;
import pe.com.proveperu.sgc.security.domain.model.Usuario;
import pe.com.proveperu.sgc.security.infrastructure.persistence.UsuarioRepository;

@Service
@RequiredArgsConstructor
public class AutenticacionService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final JwtTokenService jwtTokenService;

    @Transactional
    public LoginResponse iniciarSesion(LoginRequest request) {
        String usuarioLogin = normalizarLogin(request.usuarioLogin());
        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(usuarioLogin, request.password())
            );
        } catch (AuthenticationException exception) {
            throw new CredencialesInvalidasException();
        }

        Usuario usuario = usuarioRepository.findByUsuarioLoginIgnoreCase(authentication.getName())
            .orElseThrow(CredencialesInvalidasException::new);
        usuario.setUltimoAcceso(Instant.now());

        String token = jwtTokenService.generar(authentication, usuario);
        return new LoginResponse(token, "Bearer", UsuarioSesionResponse.from(usuario));
    }

    @Transactional(readOnly = true)
    public UsuarioSesionResponse obtenerUsuarioActual(String usuarioLogin) {
        Usuario usuario = usuarioRepository.findByUsuarioLoginIgnoreCase(usuarioLogin)
            .orElseThrow(CredencialesInvalidasException::new);
        return UsuarioSesionResponse.from(usuario);
    }

    private String normalizarLogin(String usuarioLogin) {
        return usuarioLogin.trim().toLowerCase(Locale.ROOT);
    }
}
