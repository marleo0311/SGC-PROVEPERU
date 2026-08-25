package pe.com.proveperu.sgc.cuentacobrar.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import pe.com.proveperu.sgc.cliente.domain.model.Cliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoPersona;
import pe.com.proveperu.sgc.venta.domain.model.CondicionPagoVenta;
import pe.com.proveperu.sgc.venta.domain.model.CuentaCobrar;
import pe.com.proveperu.sgc.venta.domain.model.EstadoCuentaCobrar;
import pe.com.proveperu.sgc.venta.domain.model.OrigenCuentaCobrar;
import pe.com.proveperu.sgc.venta.domain.model.TipoComprobanteVenta;
import pe.com.proveperu.sgc.venta.domain.model.Venta;

public record CuentaCobrarResumenResponse(
    Long id,
    Long idVenta,
    OrigenCuentaCobrar origen,
    Long idCliente,
    String clienteDocumento,
    String cliente,
    Instant fechaVenta,
    LocalDate fechaOrigen,
    Instant fechaRegistro,
    TipoComprobanteVenta tipoComprobante,
    String numeroComprobante,
    CondicionPagoVenta condicionPago,
    String documentoReferencia,
    String observacion,
    String usuarioCreacion,
    BigDecimal total,
    BigDecimal importePagado,
    BigDecimal saldoPendiente,
    LocalDate fechaVencimiento,
    EstadoCuentaCobrar estado
) {
    public static CuentaCobrarResumenResponse from(CuentaCobrar cuenta) {
        Venta venta = cuenta.getVenta();
        Cliente cliente = cuenta.getCliente();
        return new CuentaCobrarResumenResponse(
            cuenta.getId(),
            venta == null ? null : venta.getId(),
            cuenta.getOrigen(),
            cliente == null ? null : cliente.getId(),
            cliente == null ? null : cliente.getNumeroDocumento(),
            nombreCliente(cliente),
            venta == null ? null : venta.getFechaHora(),
            cuenta.getFechaOrigen(),
            cuenta.getFechaRegistro(),
            venta == null ? null : venta.getTipoComprobante(),
            venta == null ? null : venta.getNumeroComprobante(),
            venta == null ? null : venta.getCondicionPago(),
            cuenta.getDocumentoReferencia(),
            cuenta.getObservacion(),
            cuenta.getUsuarioCreacion().getUsuarioLogin(),
            cuenta.getTotal(),
            cuenta.getImportePagado(),
            cuenta.getSaldoPendiente(),
            cuenta.getFechaVencimiento(),
            cuenta.getEstado()
        );
    }

    private static String nombreCliente(Cliente cliente) {
        if (cliente == null) {
            return null;
        }
        return cliente.getTipoPersona() == TipoPersona.NATURAL
            ? cliente.getNombres() + " " + cliente.getApellidos()
            : cliente.getRazonSocial();
    }
}
