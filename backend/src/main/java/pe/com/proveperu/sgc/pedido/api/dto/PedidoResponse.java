package pe.com.proveperu.sgc.pedido.api.dto;

import java.time.Instant;
import java.util.List;

public record PedidoResponse(
    PedidoResumenResponse pedido,
    List<PedidoDetalleResponse> detalles,
    List<ReservaStockResponse> reservas,
    Instant fechaActualizacion
) {
}
