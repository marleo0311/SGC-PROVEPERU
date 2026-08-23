package pe.com.proveperu.sgc.pedido.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import pe.com.proveperu.sgc.pedido.domain.model.CanalPedido;

public record PedidoGuardarRequest(
    @Positive(message = "El cliente debe ser válido")
    Long idCliente,

    @Positive(message = "La sede debe ser válida")
    Long idSede,

    @NotNull(message = "El canal es obligatorio")
    CanalPedido canal,

    @NotNull(message = "Debe indicar si el precio incluye IGV")
    Boolean aplicarIgv,

    @Size(max = 300, message = "La observación no puede superar 300 caracteres")
    String observacion,

    @NotEmpty(message = "El pedido debe contener al menos un producto")
    @Size(max = 200, message = "El pedido no puede superar 200 productos")
    List<@Valid PedidoDetalleRequest> detalles
) {
}
