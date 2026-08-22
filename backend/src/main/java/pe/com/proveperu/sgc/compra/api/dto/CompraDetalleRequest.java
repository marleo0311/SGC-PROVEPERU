package pe.com.proveperu.sgc.compra.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record CompraDetalleRequest(
    @NotNull(message = "El producto es obligatorio")
    @Positive(message = "El producto debe ser válido")
    Long idProducto,

    @NotNull(message = "La unidad de medida es obligatoria")
    @Positive(message = "La unidad de medida debe ser válida")
    Long idUnidadMedida,

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor que cero")
    @Digits(
        integer = 11,
        fraction = 3,
        message = "La cantidad admite hasta 11 enteros y 3 decimales"
    )
    BigDecimal cantidad,

    @NotNull(message = "El precio de compra es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio de compra debe ser mayor que cero")
    @Digits(
        integer = 12,
        fraction = 2,
        message = "El precio admite hasta 12 enteros y 2 decimales"
    )
    BigDecimal precioCompra
) {
}
