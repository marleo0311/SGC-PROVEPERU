package pe.com.proveperu.sgc.catalogo.api.dto;

import pe.com.proveperu.sgc.catalogo.domain.model.UnidadMedida;

public record UnidadMedidaResponse(
    Long id,
    String codigo,
    String nombre,
    boolean permiteDecimales,
    String estado
) {
    public static UnidadMedidaResponse from(UnidadMedida unidad) {
        return new UnidadMedidaResponse(
            unidad.getId(),
            unidad.getCodigo(),
            unidad.getNombre(),
            unidad.isPermiteDecimales(),
            unidad.getEstado().name()
        );
    }
}
