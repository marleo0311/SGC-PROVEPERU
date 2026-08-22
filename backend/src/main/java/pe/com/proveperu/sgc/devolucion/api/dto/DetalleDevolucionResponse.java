package pe.com.proveperu.sgc.devolucion.api.dto;

import java.math.BigDecimal;
import pe.com.proveperu.sgc.devolucion.domain.model.DetalleDevolucion;
import pe.com.proveperu.sgc.devolucion.domain.model.EstadoProductoDevuelto;

public record DetalleDevolucionResponse(
    Long id,
    Long idDetalleVenta,
    Long idProducto,
    String codigoProducto,
    String producto,
    Long idUnidadMedida,
    String unidadMedida,
    BigDecimal cantidad,
    BigDecimal cantidadBase,
    EstadoProductoDevuelto estadoProducto,
    boolean reincorporadoInventario,
    BigDecimal importeDevolucion,
    BigDecimal importeReembolso,
    BigDecimal descuentoAplicado
) {
    public static DetalleDevolucionResponse from(DetalleDevolucion detalle) {
        return new DetalleDevolucionResponse(
            detalle.getId(),
            detalle.getDetalleVenta().getId(),
            detalle.getProducto().getId(),
            detalle.getProducto().getCodigoInterno(),
            detalle.getProducto().getNombre(),
            detalle.getUnidadMedida().getId(),
            detalle.getUnidadMedida().getNombre(),
            detalle.getCantidad(),
            detalle.getCantidadBase(),
            detalle.getEstadoProducto(),
            detalle.getEstadoProducto() == EstadoProductoDevuelto.APTO,
            detalle.getImporteDevolucion(),
            detalle.getImporteReembolso(),
            detalle.getDescuentoAplicado()
        );
    }
}
