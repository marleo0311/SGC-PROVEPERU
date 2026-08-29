package pe.com.proveperu.sgc.catalogo.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UnidadMedidaCrearRequest(
    @NotBlank(message = "El código de la unidad es obligatorio")
    @Size(max = 20, message = "El código de la unidad no puede superar 20 caracteres")
    String codigo,

    @NotBlank(message = "El nombre de la unidad es obligatorio")
    @Size(max = 80, message = "El nombre de la unidad no puede superar 80 caracteres")
    String nombre,

    @Size(min = 2, max = 3, message = "El código SUNAT debe tener entre 2 y 3 caracteres")
    String codigoSunat,

    @NotNull(message = "Debe indicar si la unidad permite decimales")
    Boolean permiteDecimales
) {
}
