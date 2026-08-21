package pe.com.proveperu.sgc.catalogo.api.dto;

import pe.com.proveperu.sgc.catalogo.domain.model.Marca;

public record MarcaResponse(
    Long id,
    String nombre,
    String estado
) {
    public static MarcaResponse from(Marca marca) {
        return new MarcaResponse(marca.getId(), marca.getNombre(), marca.getEstado().name());
    }
}
