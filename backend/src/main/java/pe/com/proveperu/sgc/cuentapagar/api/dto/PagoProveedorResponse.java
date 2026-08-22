package pe.com.proveperu.sgc.cuentapagar.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import pe.com.proveperu.sgc.cuentapagar.domain.model.PagoProveedor;

public record PagoProveedorResponse(
    Long id,
    Long idCuentaPagar,
    Long idMetodoPago,
    String metodoPagoCodigo,
    String metodoPago,
    Long idUsuario,
    String usuarioLogin,
    BigDecimal monto,
    String referencia,
    Instant fechaHora
) {
    public static PagoProveedorResponse from(PagoProveedor pago) {
        return new PagoProveedorResponse(
            pago.getId(),
            pago.getCuentaPagar().getId(),
            pago.getMetodoPago().getId(),
            pago.getMetodoPago().getCodigo(),
            pago.getMetodoPago().getNombre(),
            pago.getUsuario().getId(),
            pago.getUsuario().getUsuarioLogin(),
            pago.getMonto(),
            pago.getReferencia(),
            pago.getFechaHora()
        );
    }
}
