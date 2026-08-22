package pe.com.proveperu.sgc.devolucion.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CambioDevolucionRequest(
    @NotEmpty(message = "Debe incluir al menos un producto de reemplazo")
    @Size(max = 200, message = "El cambio no puede superar 200 productos")
    List<@Valid CambioItemRequest> items,

    @Positive(message = "El método de pago no es válido")
    Long idMetodoPago,

    @Size(max = 120, message = "La referencia admite hasta 120 caracteres")
    String referencia
) {
}
