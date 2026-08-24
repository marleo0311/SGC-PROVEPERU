package pe.com.proveperu.sgc.facturacionelectronica.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BajaComprobanteRequest(
    @NotBlank @Size(min = 5, max = 300) String motivo
) {
}
