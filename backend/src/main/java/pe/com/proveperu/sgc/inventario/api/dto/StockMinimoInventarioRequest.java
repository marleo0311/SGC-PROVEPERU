package pe.com.proveperu.sgc.inventario.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record StockMinimoInventarioRequest(
    @NotNull(message = "El almacén es obligatorio")
    @Positive(message = "El almacén debe ser válido")
    Long idSede,

    @NotNull(message = "El stock mínimo es obligatorio")
    @DecimalMin(value = "0.000", message = "El stock mínimo no puede ser negativo")
    @Digits(integer = 11, fraction = 3, message = "El stock mínimo admite hasta 11 enteros y 3 decimales")
    BigDecimal stockMinimo
) {
}
