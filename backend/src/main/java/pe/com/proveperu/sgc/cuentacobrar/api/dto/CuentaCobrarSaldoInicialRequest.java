package pe.com.proveperu.sgc.cuentacobrar.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CuentaCobrarSaldoInicialRequest(
    @NotNull @Positive Long idCliente,
    @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2)
    BigDecimal saldo,
    @NotNull @PastOrPresent LocalDate fechaOrigen,
    LocalDate fechaVencimiento,
    @Size(max = 120) String documentoReferencia,
    @Size(max = 500) String observacion
) {
}
