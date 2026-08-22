package pe.com.proveperu.sgc.compra.api.dto;

import java.math.BigDecimal;
import pe.com.proveperu.sgc.compra.domain.model.DetalleCompra;

public record CompraDetalleResponse(
    Long id,
    Long idProducto,
    String codigoProducto,
    String producto,
    Long idUnidadMedida,
    String codigoUnidad,
    String unidadMedida,
    BigDecimal cantidad,
    BigDecimal cantidadRecibida,
    BigDecimal cantidadPendiente,
    BigDecimal precioCompra,
    BigDecimal subtotal
) {
    public static CompraDetalleResponse from(DetalleCompra detalle) {
        return from(detalle, BigDecimal.ZERO.setScale(3));
    }

    public static CompraDetalleResponse from(
        DetalleCompra detalle,
        BigDecimal cantidadRecibida
    ) {
        return new CompraDetalleResponse(
            detalle.getId(),
            detalle.getProducto().getId(),
            detalle.getProducto().getCodigoInterno(),
            detalle.getProducto().getNombre(),
            detalle.getUnidadMedida().getId(),
            detalle.getUnidadMedida().getCodigo(),
            detalle.getUnidadMedida().getNombre(),
            detalle.getCantidad(),
            cantidadRecibida,
            detalle.getCantidad().subtract(cantidadRecibida),
            detalle.getPrecioCompra(),
            detalle.getSubtotal()
        );
    }
}
