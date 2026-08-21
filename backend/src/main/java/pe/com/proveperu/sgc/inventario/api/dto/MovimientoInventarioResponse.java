package pe.com.proveperu.sgc.inventario.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import pe.com.proveperu.sgc.inventario.domain.model.MovimientoInventario;
import pe.com.proveperu.sgc.inventario.domain.model.TipoMovimientoInventario;

public record MovimientoInventarioResponse(
    Long id,
    Long idSede,
    String nombreSede,
    Long idProducto,
    String codigoProducto,
    String nombreProducto,
    TipoMovimientoInventario tipoMovimiento,
    BigDecimal cantidad,
    Long idUnidadMedida,
    String codigoUnidadMedida,
    BigDecimal cantidadBase,
    String codigoUnidadBase,
    BigDecimal stockAnterior,
    BigDecimal stockResultante,
    String documentoOrigen,
    Long idOrigen,
    String motivo,
    Long idUsuario,
    String usuarioLogin,
    String nombreUsuario,
    Instant fechaHora
) {
    public static MovimientoInventarioResponse from(MovimientoInventario movimiento) {
        return new MovimientoInventarioResponse(
            movimiento.getId(),
            movimiento.getSede().getId(),
            movimiento.getSede().getNombre(),
            movimiento.getProducto().getId(),
            movimiento.getProducto().getCodigoInterno(),
            movimiento.getProducto().getNombre(),
            movimiento.getTipoMovimiento(),
            movimiento.getCantidad(),
            movimiento.getUnidadMedida().getId(),
            movimiento.getUnidadMedida().getCodigo(),
            movimiento.getCantidadBase(),
            movimiento.getProducto().getUnidadBase().getCodigo(),
            movimiento.getStockAnterior(),
            movimiento.getStockResultante(),
            movimiento.getDocumentoOrigen(),
            movimiento.getIdOrigen(),
            movimiento.getMotivo(),
            movimiento.getUsuario().getId(),
            movimiento.getUsuario().getUsuarioLogin(),
            movimiento.getUsuario().getNombreCompleto(),
            movimiento.getFechaHora()
        );
    }
}
