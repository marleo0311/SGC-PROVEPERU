package pe.com.proveperu.sgc.facturacionelectronica.api.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record ResumenDiarioCrearRequest(
    @NotNull LocalDate fechaEmision
) {
}
