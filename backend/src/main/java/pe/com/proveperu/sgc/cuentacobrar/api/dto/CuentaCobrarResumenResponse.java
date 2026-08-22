package pe.com.proveperu.sgc.cuentacobrar.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import pe.com.proveperu.sgc.cliente.domain.model.Cliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoPersona;
import pe.com.proveperu.sgc.venta.domain.model.CondicionPagoVenta;
import pe.com.proveperu.sgc.venta.domain.model.CuentaCobrar;
import pe.com.proveperu.sgc.venta.domain.model.EstadoCuentaCobrar;
import pe.com.proveperu.sgc.venta.domain.model.TipoComprobanteVenta;
import pe.com.proveperu.sgc.venta.domain.model.Venta;

public record CuentaCobrarResumenResponse(
    Long id,
    Long idVenta,
    Long idCliente,
    String clienteDocumento,
    String cliente,
    Instant fechaVenta,
    TipoComprobanteVenta tipoComprobante,
    String numeroComprobante,
    CondicionPagoVenta condicionPago,
    BigDecimal total,
    BigDecimal importePagado,
    BigDecimal saldoPendiente,
    LocalDate fechaVencimiento,
    EstadoCuentaCobrar estado
) {
    public static CuentaCobrarResumenResponse from(CuentaCobrar cuenta) {
        Venta venta = cuenta.getVenta();
        Cliente cliente = venta.getCliente();
        return new CuentaCobrarResumenResponse(
            cuenta.getId(),
            venta.getId(),
            cliente == null ? null : cliente.getId(),
            cliente == null ? null : cliente.getNumeroDocumento(),
            nombreCliente(cliente),
            venta.getFechaHora(),
            venta.getTipoComprobante(),
            venta.getNumeroComprobante(),
            venta.getCondicionPago(),
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
