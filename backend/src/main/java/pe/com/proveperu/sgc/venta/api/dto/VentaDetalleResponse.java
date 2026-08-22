package pe.com.proveperu.sgc.venta.api.dto;

import java.math.BigDecimal;
import pe.com.proveperu.sgc.venta.domain.model.DetalleVenta;

public record VentaDetalleResponse(
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
    public static VentaDetalleResponse from(DetalleVenta detalle) {
        return new VentaDetalleResponse(
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
