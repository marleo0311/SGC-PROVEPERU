package pe.com.proveperu.sgc.security.api.dto;

import java.time.Instant;
import pe.com.proveperu.sgc.security.domain.model.Usuario;

public record UsuarioResponse(
    Long id,
    String nombreCompleto,
    String usuarioLogin,
    String estado,
    RolResumenResponse rol,
    Instant ultimoAcceso,
    Instant fechaRegistro
) {
    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
            usuario.getId(),
            usuario.getNombreCompleto(),
            usuario.getUsuarioLogin(),
            usuario.getEstado().name(),
            RolResumenResponse.from(usuario.getRol()),
            usuario.getUltimoAcceso(),
            usuario.getFechaRegistro()
        );
    }
}
