package pe.com.proveperu.sgc.devolucion.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import pe.com.proveperu.sgc.devolucion.domain.model.TipoSolucionDevolucion;

public record DevolucionCrearRequest(
    @NotNull(message = "La venta es obligatoria")
    @Positive(message = "La venta no es válida")
    Long idVenta,

    @NotBlank(message = "El motivo es obligatorio")
    @Size(max = 300, message = "El motivo admite hasta 300 caracteres")
    String motivo,

    @NotNull(message = "El tipo de solución es obligatorio")
    TipoSolucionDevolucion tipoSolucion,

    @NotEmpty(message = "Debe incluir al menos un producto devuelto")
    List<@Valid DevolucionItemRequest> items
) {
}
