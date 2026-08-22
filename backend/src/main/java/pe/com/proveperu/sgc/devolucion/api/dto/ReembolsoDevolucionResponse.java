package pe.com.proveperu.sgc.devolucion.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import pe.com.proveperu.sgc.devolucion.domain.model.ReembolsoDevolucion;

public record ReembolsoDevolucionResponse(
    Long id,
    Long idMetodoPago,
    String metodoPagoCodigo,
    String metodoPago,
    Long idUsuario,
    String usuarioLogin,
    BigDecimal importe,
    String referencia,
    Instant fechaHora
) {
    public static ReembolsoDevolucionResponse from(
        ReembolsoDevolucion reembolso
    ) {
        return new ReembolsoDevolucionResponse(
            reembolso.getId(),
            reembolso.getMetodoPago().getId(),
            reembolso.getMetodoPago().getCodigo(),
            reembolso.getMetodoPago().getNombre(),
            reembolso.getUsuario().getId(),
            reembolso.getUsuario().getUsuarioLogin(),
            reembolso.getImporte(),
            reembolso.getReferencia(),
            reembolso.getFechaHora()
        );
    }
}
