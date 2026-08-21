package pe.com.proveperu.sgc.catalogo.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MarcaCrearRequest(
    @NotBlank(message = "El nombre de la marca es obligatorio")
    @Size(max = 120, message = "El nombre de la marca no puede superar 120 caracteres")
    String nombre
) {
}
