package pe.com.proveperu.sgc.venta.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import pe.com.proveperu.sgc.venta.domain.model.PagoCliente;

public record PagoClienteResponse(
    Long id,
    Long idCuentaCobrar,
    Long idMetodoPago,
    String metodoPagoCodigo,
    String metodoPago,
    Long idUsuario,
    String usuarioLogin,
    BigDecimal monto,
    String referencia,
    Instant fechaHora
) {
    public static PagoClienteResponse from(PagoCliente pago) {
        return new PagoClienteResponse(
            pago.getId(),
            pago.getCuentaCobrar() == null ? null : pago.getCuentaCobrar().getId(),
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
