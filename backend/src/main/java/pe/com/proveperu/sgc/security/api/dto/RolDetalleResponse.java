package pe.com.proveperu.sgc.security.api.dto;

import java.util.Comparator;
import java.util.List;
import pe.com.proveperu.sgc.security.domain.model.Rol;

public record RolDetalleResponse(
    Long id,
    String nombre,
    String descripcion,
    String estado,
    List<PermisoResponse> permisos
) {
    public static RolDetalleResponse from(Rol rol) {
        List<PermisoResponse> permisos = rol.getPermisos().stream()
            .sorted(Comparator.comparing(p -> p.getCodigo().toLowerCase()))
            .map(PermisoResponse::from)
            .toList();
        return new RolDetalleResponse(
            rol.getId(),
            rol.getNombre(),
            rol.getDescripcion(),
            rol.getEstado().name(),
            permisos
        );
    }
}
