package pe.com.proveperu.sgc.pedido.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import pe.com.proveperu.sgc.cliente.domain.model.Cliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoPersona;
import pe.com.proveperu.sgc.pedido.domain.model.CanalPedido;
import pe.com.proveperu.sgc.pedido.domain.model.EstadoPedido;
import pe.com.proveperu.sgc.pedido.domain.model.Pedido;

public record PedidoResumenResponse(
    Long id,
    Long idCliente,
    String clienteDocumento,
    String cliente,
    Long idCotizacion,
    Long idUsuario,
    String usuarioLogin,
    Long idSede,
    String sede,
    CanalPedido canal,
    Instant fechaHora,
    EstadoPedido estado,
    String observacion,
    BigDecimal subtotal,
    BigDecimal igv,
    BigDecimal total
) {
    public static PedidoResumenResponse from(Pedido pedido) {
        Cliente cliente = pedido.getCliente();
        return new PedidoResumenResponse(
            pedido.getId(),
            cliente == null ? null : cliente.getId(),
            cliente == null ? null : cliente.getNumeroDocumento(),
            nombreCliente(cliente),
            pedido.getCotizacion() == null ? null : pedido.getCotizacion().getId(),
            pedido.getUsuario().getId(),
            pedido.getUsuario().getUsuarioLogin(),
            pedido.getSede().getId(),
            pedido.getSede().getNombre(),
            pedido.getCanal(),
            pedido.getFechaHora(),
            pedido.getEstado(),
            pedido.getObservacion(),
            pedido.getSubtotal(),
            pedido.getIgv(),
            pedido.getTotal()
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
