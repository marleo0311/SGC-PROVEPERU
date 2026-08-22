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
    BigDecimal precioCompra,
    BigDecimal subtotal
) {
    public static CompraDetalleResponse from(DetalleCompra detalle) {
        return new CompraDetalleResponse(
            detalle.getId(),
            detalle.getProducto().getId(),
            detalle.getProducto().getCodigoInterno(),
            detalle.getProducto().getNombre(),
            detalle.getUnidadMedida().getId(),
            detalle.getUnidadMedida().getCodigo(),
            detalle.getUnidadMedida().getNombre(),
            detalle.getCantidad(),
            detalle.getPrecioCompra(),
            detalle.getSubtotal()
        );
    }
}
