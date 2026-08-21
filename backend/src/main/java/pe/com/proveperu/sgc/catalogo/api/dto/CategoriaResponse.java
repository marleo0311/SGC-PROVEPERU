package pe.com.proveperu.sgc.catalogo.api.dto;

import pe.com.proveperu.sgc.catalogo.domain.model.Categoria;

public record CategoriaResponse(
    Long id,
    String nombre,
    String descripcion,
    String estado
) {
    public static CategoriaResponse from(Categoria categoria) {
        return new CategoriaResponse(
            categoria.getId(),
            categoria.getNombre(),
            categoria.getDescripcion(),
            categoria.getEstado().name()
        );
    }
}
