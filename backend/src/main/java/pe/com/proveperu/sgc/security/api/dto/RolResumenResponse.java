package pe.com.proveperu.sgc.security.api.dto;

import pe.com.proveperu.sgc.security.domain.model.Rol;

public record RolResumenResponse(
    Long id,
    String nombre,
    String descripcion,
    String estado
) {
    public static RolResumenResponse from(Rol rol) {
        return new RolResumenResponse(
            rol.getId(),
            rol.getNombre(),
            rol.getDescripcion(),
            rol.getEstado().name()
        );
    }
}
