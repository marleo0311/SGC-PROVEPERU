package pe.com.proveperu.sgc.cuentacobrar.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record CuentaCobrarVencimientoRequest(
    @Schema(
        description = "Fecha de vencimiento. Enviar null para dejarla sin vencimiento definido",
        example = "2026-09-30"
    )
    LocalDate fechaVencimiento
) {
}
