package pe.com.proveperu.sgc.venta.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record VentaItemRequest(
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

    @DecimalMin(value = "0.01", message = "El precio unitario debe ser mayor que cero")
    @Digits(integer = 12, fraction = 2, message = "El precio admite hasta 12 enteros y 2 decimales")
    @Schema(
        description = "Precio esperado por el cliente; el servidor lo valida contra el precio vigente",
        example = "120.00"
    )
    BigDecimal precioUnitario,

    @NotNull(message = "El descuento es obligatorio")
    @DecimalMin(value = "0.00", message = "El descuento no puede ser negativo")
    @Digits(integer = 12, fraction = 2, message = "El descuento admite hasta 12 enteros y 2 decimales")
    BigDecimal descuento
) {
}
