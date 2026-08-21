package pe.com.proveperu.sgc.security.api.dto;

public record LoginResponse(
    String token,
    String tipo,
    UsuarioSesionResponse usuario
) {
}
