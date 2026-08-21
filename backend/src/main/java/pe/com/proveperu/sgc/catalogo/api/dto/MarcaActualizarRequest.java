package pe.com.proveperu.sgc.catalogo.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;

public record MarcaActualizarRequest(
    @NotBlank(message = "El nombre de la marca es obligatorio")
    @Size(max = 120, message = "El nombre de la marca no puede superar 120 caracteres")
    String nombre,

    @NotNull(message = "El estado es obligatorio")
    EstadoCatalogo estado
) {
}
