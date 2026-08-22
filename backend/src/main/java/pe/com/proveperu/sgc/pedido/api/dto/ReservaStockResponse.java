package pe.com.proveperu.sgc.pedido.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import pe.com.proveperu.sgc.pedido.domain.model.EstadoReservaStock;
import pe.com.proveperu.sgc.pedido.domain.model.ReservaStock;

public record ReservaStockResponse(
    Long id,
    Long idPedido,
    Long idDetallePedido,
    Long idSede,
    String sede,
    Long idProducto,
    String codigoProducto,
    String producto,
    BigDecimal cantidadBase,
    EstadoReservaStock estado,
    Instant fechaReserva,
    Instant fechaLiberacion
) {
    public static ReservaStockResponse from(ReservaStock reserva) {
        return new ReservaStockResponse(
            reserva.getId(),
            reserva.getPedido().getId(),
            reserva.getDetallePedido().getId(),
            reserva.getSede().getId(),
            reserva.getSede().getNombre(),
            reserva.getProducto().getId(),
            reserva.getProducto().getCodigoInterno(),
            reserva.getProducto().getNombre(),
            reserva.getCantidad(),
            reserva.getEstado(),
            reserva.getFechaReserva(),
            reserva.getFechaLiberacion()
        );
    }
}
