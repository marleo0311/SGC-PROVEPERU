package pe.com.proveperu.sgc.cuentacobrar.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PagoClienteRequest(
    @NotNull(message = "El método de pago es obligatorio")
    @Positive(message = "El método de pago debe ser válido")
    @Schema(description = "Identificador de un método de pago activo", example = "1")
    Long idMetodoPago,

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero")
    @Digits(integer = 12, fraction = 2, message = "El monto admite hasta 12 enteros y 2 decimales")
    @Schema(example = "250.00")
    BigDecimal monto,

    @Size(max = 120, message = "La referencia no puede superar 120 caracteres")
    @Schema(description = "Número de operación, voucher u otra referencia", example = "OP-123456")
    String referencia
) {
}
