package pe.com.proveperu.sgc.comprobante.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComprobanteAnularRequest(
    @NotBlank(message = "El motivo de anulación es obligatorio")
    @Size(max = 300, message = "El motivo no puede superar 300 caracteres")
    String motivo
) {
}
