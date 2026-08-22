package pe.com.proveperu.sgc.venta.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VentaAnularRequest(
    @NotBlank(message = "El motivo de anulación es obligatorio")
    @Size(max = 300, message = "El motivo no puede superar 300 caracteres")
    String motivo
) {
}
