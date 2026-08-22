package pe.com.proveperu.sgc.caja.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CierreCajaRequest(
    @NotNull(message = "El saldo real es obligatorio")
    @DecimalMin(value = "0.00", message = "El saldo real no puede ser negativo")
    @Digits(integer = 12, fraction = 2, message = "El saldo real admite hasta 12 enteros y 2 decimales")
    BigDecimal saldoReal,

    @Size(max = 300, message = "La observación admite hasta 300 caracteres")
    String observacion
) {
}
