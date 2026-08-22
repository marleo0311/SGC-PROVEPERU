package pe.com.proveperu.sgc.caja.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import pe.com.proveperu.sgc.caja.domain.model.ConceptoMovimientoCaja;
import pe.com.proveperu.sgc.caja.domain.model.MovimientoCaja;
import pe.com.proveperu.sgc.caja.domain.model.TipoMovimientoCaja;

public record MovimientoCajaResponse(
    Long id,
    Long idSesionCaja,
    Instant fechaHora,
    TipoMovimientoCaja tipo,
    ConceptoMovimientoCaja concepto,
    Long idMetodoPago,
    String metodoPagoCodigo,
    String metodoPago,
    BigDecimal importe,
    String referencia,
    String observacion,
    Long idOrigen,
    Long idVenta,
    String numeroComprobante,
    Long idUsuario,
    String usuarioLogin,
    Long idVendedor,
    String vendedorLogin
) {
    public static MovimientoCajaResponse from(MovimientoCaja movimiento) {
        Long idVenta = movimiento.getVenta() == null
            ? null
            : movimiento.getVenta().getId();
        String numeroComprobante = idVenta == null
            ? null
            : movimiento.getVenta().getNumeroComprobante();
        Long idVendedor = movimiento.getVendedor() == null
            ? null
            : movimiento.getVendedor().getId();
        String vendedorLogin = movimiento.getVendedor() == null
            ? null
            : movimiento.getVendedor().getUsuarioLogin();
        return new MovimientoCajaResponse(
            movimiento.getId(),
            movimiento.getSesion().getId(),
            movimiento.getFechaHora(),
            movimiento.getTipo(),
            movimiento.getConcepto(),
            movimiento.getMetodoPago().getId(),
            movimiento.getMetodoPago().getCodigo(),
            movimiento.getMetodoPago().getNombre(),
            movimiento.getImporte(),
            movimiento.getReferencia(),
            movimiento.getObservacion(),
            movimiento.getIdOrigen(),
            idVenta,
            numeroComprobante,
            movimiento.getUsuario().getId(),
            movimiento.getUsuario().getUsuarioLogin(),
            idVendedor,
            vendedorLogin
        );
    }
}
