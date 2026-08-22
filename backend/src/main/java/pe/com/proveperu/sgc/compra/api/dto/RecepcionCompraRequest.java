package pe.com.proveperu.sgc.compra.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RecepcionCompraRequest(
    @Positive(message = "La sede debe ser válida")
    Long idSede,

    @NotEmpty(message = "La recepción debe contener al menos un producto")
    @Size(max = 200, message = "La recepción no puede superar 200 productos")
    List<@Valid RecepcionCompraItemRequest> items,

    @Size(max = 300, message = "La observación no puede superar 300 caracteres")
    String observacion
) {
}
