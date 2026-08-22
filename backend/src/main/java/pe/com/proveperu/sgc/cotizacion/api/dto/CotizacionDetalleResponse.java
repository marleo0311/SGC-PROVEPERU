package pe.com.proveperu.sgc.cotizacion.api.dto;

import java.math.BigDecimal;

public record CotizacionDetalleResponse(
    Long id,
    Long idProducto,
    String codigoProducto,
    String producto,
    Long idUnidadMedida,
    String unidadCodigo,
    String unidadMedida,
    BigDecimal cantidad,
    BigDecimal precioUnitario,
    BigDecimal descuento,
    BigDecimal subtotal,
    BigDecimal cantidadBase,
    BigDecimal stockDisponibleBase,
    BigDecimal stockDisponible,
    boolean disponible
) {
}
