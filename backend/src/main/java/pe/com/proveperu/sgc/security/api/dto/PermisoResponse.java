package pe.com.proveperu.sgc.security.api.dto;

import pe.com.proveperu.sgc.security.domain.model.Permiso;

public record PermisoResponse(
    Long id,
    String codigo,
    String nombre,
    String modulo,
    String descripcion
) {
    public static PermisoResponse from(Permiso permiso) {
        return new PermisoResponse(
            permiso.getId(),
            permiso.getCodigo(),
            permiso.getNombre(),
            permiso.getModulo(),
            permiso.getDescripcion()
        );
    }
}
