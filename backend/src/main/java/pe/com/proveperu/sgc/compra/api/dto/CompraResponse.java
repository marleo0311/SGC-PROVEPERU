package pe.com.proveperu.sgc.compra.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import pe.com.proveperu.sgc.compra.domain.model.Compra;
import pe.com.proveperu.sgc.compra.domain.model.CondicionPagoCompra;
import pe.com.proveperu.sgc.compra.domain.model.EstadoCompra;

public record CompraResponse(
    Long id,
    Long idProveedor,
    String rucProveedor,
    String proveedor,
    Long idUsuario,
    String usuarioLogin,
    LocalDate fecha,
    String tipoComprobante,
    String numeroComprobante,
    CondicionPagoCompra condicionPago,
    BigDecimal subtotal,
    BigDecimal igv,
    BigDecimal gastosAdicionales,
    BigDecimal total,
    EstadoCompra estado,
    List<CompraDetalleResponse> detalles,
    Instant fechaRegistro,
    Instant fechaActualizacion
) {
    public static CompraResponse from(Compra compra) {
        return new CompraResponse(
            compra.getId(),
            compra.getProveedor().getId(),
            compra.getProveedor().getRuc(),
            compra.getProveedor().getRazonSocial(),
            compra.getUsuario().getId(),
            compra.getUsuario().getUsuarioLogin(),
            compra.getFecha(),
            compra.getTipoComprobante(),
            compra.getNumeroComprobante(),
            compra.getCondicionPago(),
            compra.getSubtotal(),
            compra.getIgv(),
            compra.getGastosAdicionales(),
            compra.getTotal(),
            compra.getEstado(),
            compra.getDetalles().stream().map(CompraDetalleResponse::from).toList(),
            compra.getFechaRegistro(),
            compra.getFechaActualizacion()
        );
    }
}
