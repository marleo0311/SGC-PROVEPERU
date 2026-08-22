package pe.com.proveperu.sgc.pedido.api.dto;

import java.math.BigDecimal;
import pe.com.proveperu.sgc.pedido.domain.model.DetallePedido;

public record PedidoDetalleResponse(
    Long id,
    Long idProducto,
    String codigoProducto,
    String producto,
    Long idUnidadMedida,
    String unidadCodigo,
    String unidadMedida,
    BigDecimal cantidad,
    BigDecimal cantidadBase,
    BigDecimal precioUnitario,
    BigDecimal descuento,
    BigDecimal subtotal
) {
    public static PedidoDetalleResponse from(DetallePedido detalle) {
        return new PedidoDetalleResponse(
            detalle.getId(),
            detalle.getProducto().getId(),
            detalle.getProducto().getCodigoInterno(),
            detalle.getProducto().getNombre(),
            detalle.getUnidadMedida().getId(),
            detalle.getUnidadMedida().getCodigo(),
            detalle.getUnidadMedida().getNombre(),
            detalle.getCantidad(),
            detalle.getCantidadBase(),
            detalle.getPrecioUnitario(),
            detalle.getDescuento(),
            detalle.getSubtotal()
        );
    }
}
