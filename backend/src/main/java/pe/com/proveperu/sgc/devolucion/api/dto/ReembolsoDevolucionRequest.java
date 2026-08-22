package pe.com.proveperu.sgc.devolucion.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ReembolsoDevolucionRequest(
    @NotNull(message = "El método de pago es obligatorio")
    @Positive(message = "El método de pago no es válido")
    Long idMetodoPago,

    @NotNull(message = "El importe es obligatorio")
    @DecimalMin(value = "0.00", inclusive = false, message = "El importe debe ser mayor que cero")
    @Digits(integer = 12, fraction = 2, message = "El importe admite hasta 12 enteros y 2 decimales")
    BigDecimal importe,

    @Size(max = 120, message = "La referencia admite hasta 120 caracteres")
    String referencia
) {
}
