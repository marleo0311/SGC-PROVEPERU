package pe.com.proveperu.sgc.catalogo.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ProductoActualizarRequest(
    @NotBlank(message = "El código interno es obligatorio")
    @Size(max = 60, message = "El código interno no puede superar 60 caracteres")
    String codigoInterno,

    @Size(max = 80, message = "El código de barras no puede superar 80 caracteres")
    String codigoBarras,

    @NotBlank(message = "El nombre del producto es obligatorio")
    @Size(max = 180, message = "El nombre del producto no puede superar 180 caracteres")
    String nombre,

    @Size(max = 300, message = "La descripción no puede superar 300 caracteres")
    String descripcion,

    @NotNull(message = "La categoría es obligatoria")
    @Positive(message = "La categoría debe ser válida")
    Long idCategoria,

    @Positive(message = "La marca debe ser válida")
    Long idMarca,

    @NotNull(message = "La unidad base es obligatoria")
    @Positive(message = "La unidad base debe ser válida")
    Long idUnidadBase,

    @NotNull(message = "El stock mínimo es obligatorio")
    @DecimalMin(value = "0.000", message = "El stock mínimo no puede ser negativo")
    @Digits(integer = 11, fraction = 3, message = "El stock mínimo debe tener hasta 11 enteros y 3 decimales")
    BigDecimal stockMinimo,

    @DecimalMin(value = "0.01", message = "El precio minorista debe ser mayor que cero")
    @Digits(integer = 12, fraction = 2, message = "El precio minorista debe tener hasta 12 enteros y 2 decimales")
    BigDecimal precioMinorista,

    @DecimalMin(value = "0.01", message = "El precio mayorista debe ser mayor que cero")
    @Digits(integer = 12, fraction = 2, message = "El precio mayorista debe tener hasta 12 enteros y 2 decimales")
    BigDecimal precioMayorista
) {
}
