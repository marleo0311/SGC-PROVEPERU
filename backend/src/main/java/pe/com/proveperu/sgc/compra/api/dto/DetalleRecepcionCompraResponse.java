package pe.com.proveperu.sgc.compra.api.dto;

import java.math.BigDecimal;
import pe.com.proveperu.sgc.compra.domain.model.DetalleRecepcionCompra;

public record DetalleRecepcionCompraResponse(
    Long id,
    Long idDetalleCompra,
    Long idProducto,
    String codigoProducto,
    String producto,
    Long idUnidadMedida,
    String codigoUnidad,
    String unidadMedida,
    BigDecimal cantidadEsperada,
    BigDecimal cantidadRecibida,
    BigDecimal cantidadAcumulada,
    BigDecimal cantidadPendiente,
    boolean conforme,
    String observacion
) {
    public static DetalleRecepcionCompraResponse from(DetalleRecepcionCompra detalle) {
        return new DetalleRecepcionCompraResponse(
            detalle.getId(),
            detalle.getDetalleCompra().getId(),
            detalle.getProducto().getId(),
            detalle.getProducto().getCodigoInterno(),
            detalle.getProducto().getNombre(),
            detalle.getUnidadMedida().getId(),
            detalle.getUnidadMedida().getCodigo(),
            detalle.getUnidadMedida().getNombre(),
            detalle.getCantidadEsperada(),
            detalle.getCantidadRecibida(),
            detalle.getCantidadAcumulada(),
            detalle.getCantidadPendiente(),
            detalle.isConforme(),
            detalle.getObservacion()
        );
    }
}
