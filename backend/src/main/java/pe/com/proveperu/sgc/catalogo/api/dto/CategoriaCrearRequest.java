package pe.com.proveperu.sgc.catalogo.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaCrearRequest(
    @NotBlank(message = "El nombre de la categoría es obligatorio")
    @Size(max = 120, message = "El nombre de la categoría no puede superar 120 caracteres")
    String nombre,

    @Size(max = 250, message = "La descripción no puede superar 250 caracteres")
    String descripcion
) {
}
