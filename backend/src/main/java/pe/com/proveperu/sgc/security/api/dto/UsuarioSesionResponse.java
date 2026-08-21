package pe.com.proveperu.sgc.security.api.dto;

import pe.com.proveperu.sgc.security.application.service.RolAuthorityMapper;
import pe.com.proveperu.sgc.security.domain.model.Usuario;

public record UsuarioSesionResponse(
    Long idUsuario,
    String usuarioLogin,
    String nombreCompleto,
    String rol
) {

    public static UsuarioSesionResponse from(Usuario usuario) {
        return new UsuarioSesionResponse(
            usuario.getId(),
            usuario.getUsuarioLogin(),
            usuario.getNombreCompleto(),
            RolAuthorityMapper.normalizar(usuario.getRol().getNombre())
        );
    }
}
