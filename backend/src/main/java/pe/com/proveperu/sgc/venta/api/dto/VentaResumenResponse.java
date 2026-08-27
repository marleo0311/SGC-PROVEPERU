package pe.com.proveperu.sgc.venta.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import pe.com.proveperu.sgc.cliente.domain.model.Cliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoPersona;
import pe.com.proveperu.sgc.venta.domain.model.CondicionPagoVenta;
import pe.com.proveperu.sgc.venta.domain.model.CuentaCobrar;
import pe.com.proveperu.sgc.venta.domain.model.EstadoVenta;
import pe.com.proveperu.sgc.venta.domain.model.TipoComprobanteVenta;
import pe.com.proveperu.sgc.venta.domain.model.TipoVenta;
import pe.com.proveperu.sgc.venta.domain.model.Venta;

public record VentaResumenResponse(
    Long id,
    Long idCliente,
    String clienteDocumento,
    String cliente,
    Long idVendedor,
    String vendedorLogin,
    Long idPedido,
    Long idSede,
    String sede,
    Long idAlmacenSalida,
    String almacenSalida,
    Instant fechaHora,
    TipoVenta tipoVenta,
    CondicionPagoVenta condicionPago,
    TipoComprobanteVenta tipoComprobante,
    String numeroComprobante,
    BigDecimal subtotal,
    BigDecimal igv,
    BigDecimal descuentoTotal,
    BigDecimal total,
    BigDecimal importePagado,
    BigDecimal saldoPendiente,
    EstadoVenta estado,
    Instant fechaAnulacion,
    String motivoAnulacion
) {
    public static VentaResumenResponse from(Venta venta) {
        Cliente cliente = venta.getCliente();
        CuentaCobrar cuenta = venta.getCuentaCobrar();
        return new VentaResumenResponse(
            venta.getId(),
            cliente == null ? null : cliente.getId(),
            cliente == null ? null : cliente.getNumeroDocumento(),
            nombreCliente(cliente),
            venta.getVendedor().getId(),
            venta.getVendedor().getUsuarioLogin(),
            venta.getPedido() == null ? null : venta.getPedido().getId(),
            venta.getAlmacenSalida().getId(),
            venta.getAlmacenSalida().getNombre(),
            venta.getAlmacenSalida().getId(),
            venta.getAlmacenSalida().getNombre(),
            venta.getFechaHora(),
            venta.getTipoVenta(),
            venta.getCondicionPago(),
            venta.getTipoComprobante(),
            venta.getNumeroComprobante(),
            venta.getSubtotal(),
            venta.getIgv(),
            venta.getDescuentoTotal(),
            venta.getTotal(),
            cuenta == null ? venta.getTotal() : cuenta.getImportePagado(),
            cuenta == null ? BigDecimal.ZERO.setScale(2) : cuenta.getSaldoPendiente(),
            venta.getEstado(),
            venta.getFechaAnulacion(),
            venta.getMotivoAnulacion()
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
