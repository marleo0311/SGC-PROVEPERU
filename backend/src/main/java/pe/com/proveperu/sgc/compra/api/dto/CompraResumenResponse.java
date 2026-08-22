package pe.com.proveperu.sgc.compra.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import pe.com.proveperu.sgc.compra.domain.model.Compra;
import pe.com.proveperu.sgc.compra.domain.model.CondicionPagoCompra;
import pe.com.proveperu.sgc.compra.domain.model.EstadoCompra;

public record CompraResumenResponse(
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
    Instant fechaRegistro,
    Instant fechaActualizacion
) {
    public static CompraResumenResponse from(Compra compra) {
        return new CompraResumenResponse(
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
            compra.getFechaRegistro(),
            compra.getFechaActualizacion()
        );
    }
}
