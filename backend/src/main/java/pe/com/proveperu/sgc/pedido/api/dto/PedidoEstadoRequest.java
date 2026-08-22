package pe.com.proveperu.sgc.pedido.api.dto;

import jakarta.validation.constraints.NotNull;
import pe.com.proveperu.sgc.pedido.domain.model.EstadoPedido;

public record PedidoEstadoRequest(
    @NotNull(message = "El estado es obligatorio")
    EstadoPedido estado
) {
}
