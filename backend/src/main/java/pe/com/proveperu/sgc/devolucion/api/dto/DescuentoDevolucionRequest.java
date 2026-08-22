package pe.com.proveperu.sgc.devolucion.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record DescuentoDevolucionRequest(
    @NotNull(message = "El importe del descuento es obligatorio")
    @DecimalMin(value = "0.01", message = "El descuento debe ser mayor que cero")
    @Digits(integer = 12, fraction = 2, message = "El descuento admite hasta 12 enteros y 2 decimales")
    BigDecimal importe,

    @Positive(message = "El método de pago no es válido")
    Long idMetodoPago,

    @Size(max = 120, message = "La referencia admite hasta 120 caracteres")
    String referencia
) {
}
