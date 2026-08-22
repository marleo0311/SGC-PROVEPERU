package pe.com.proveperu.sgc.caja.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AperturaCajaRequest(
    @NotNull(message = "El saldo inicial es obligatorio")
    @DecimalMin(value = "0.00", message = "El saldo inicial no puede ser negativo")
    @Digits(integer = 12, fraction = 2, message = "El saldo inicial admite hasta 12 enteros y 2 decimales")
    BigDecimal saldoInicial
) {
}
