package pe.com.proveperu.sgc.cotizacion.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CotizacionDetalleRequest(
    @NotNull(message = "El producto es obligatorio")
    @Positive(message = "El producto debe ser válido")
    Long idProducto,

    @NotNull(message = "La unidad de medida es obligatoria")
    @Positive(message = "La unidad de medida debe ser válida")
    Long idUnidadMedida,

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor que cero")
    @Digits(integer = 11, fraction = 3, message = "La cantidad admite hasta 11 enteros y 3 decimales")
    BigDecimal cantidad,

    @Size(max = 30, message = "El tipo de precio no puede superar 30 caracteres")
    @Pattern(
        regexp = "^[A-Za-z][A-Za-z0-9_-]{0,29}$",
        message = "El tipo de precio solo admite letras, números, guion y guion bajo"
    )
    @Schema(description = "Tipo de precio vigente; si se omite usa MINORISTA", example = "MINORISTA")
    String tipoPrecio,

    @NotNull(message = "El descuento es obligatorio")
    @DecimalMin(value = "0.00", message = "El descuento no puede ser negativo")
    @Digits(integer = 12, fraction = 2, message = "El descuento admite hasta 12 enteros y 2 decimales")
    @Schema(description = "Descuento monetario aplicado a la línea", example = "10.00")
    BigDecimal descuento
) {
}
