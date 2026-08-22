package pe.com.proveperu.sgc.inventario.api.dto;

import pe.com.proveperu.sgc.inventario.domain.model.Sede;

public record SedeResponse(
    Long id,
    String nombre,
    String direccion,
    String estado
) {
    public static SedeResponse from(Sede sede) {
        return new SedeResponse(
            sede.getId(),
            sede.getNombre(),
            sede.getDireccion(),
            sede.getEstado()
        );
    }
}
