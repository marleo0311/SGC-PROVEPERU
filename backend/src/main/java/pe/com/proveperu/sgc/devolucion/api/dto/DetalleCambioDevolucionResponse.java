package pe.com.proveperu.sgc.devolucion.api.dto;

import java.math.BigDecimal;
import pe.com.proveperu.sgc.devolucion.domain.model.DetalleCambioDevolucion;

public record DetalleCambioDevolucionResponse(
    Long id,
    Long idProducto,
    String productoCodigo,
    String productoNombre,
    Long idUnidadMedida,
    String unidadCodigo,
    BigDecimal cantidad,
    BigDecimal cantidadBase,
    BigDecimal precioUnitario,
    BigDecimal subtotal
) {
    public static DetalleCambioDevolucionResponse from(
        DetalleCambioDevolucion detalle
    ) {
        return new DetalleCambioDevolucionResponse(
            detalle.getId(),
            detalle.getProducto().getId(),
            detalle.getProducto().getCodigoInterno(),
            detalle.getProducto().getNombre(),
            detalle.getUnidadMedida().getId(),
            detalle.getUnidadMedida().getCodigo(),
            detalle.getCantidad(),
            detalle.getCantidadBase(),
            detalle.getPrecioUnitario(),
            detalle.getSubtotal()
        );
    }
}
