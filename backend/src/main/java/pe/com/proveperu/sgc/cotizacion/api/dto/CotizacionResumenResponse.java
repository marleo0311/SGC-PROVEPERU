package pe.com.proveperu.sgc.cotizacion.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import pe.com.proveperu.sgc.cliente.domain.model.Cliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoPersona;
import pe.com.proveperu.sgc.cotizacion.domain.model.Cotizacion;
import pe.com.proveperu.sgc.cotizacion.domain.model.EstadoCotizacion;

public record CotizacionResumenResponse(
    Long id,
    Long idCliente,
    String clienteDocumento,
    String cliente,
    Long idUsuario,
    String usuarioLogin,
    LocalDate fecha,
    LocalDate fechaVencimiento,
    BigDecimal subtotal,
    BigDecimal igv,
    BigDecimal total,
    EstadoCotizacion estado
) {
    public static CotizacionResumenResponse from(Cotizacion cotizacion) {
        Cliente cliente = cotizacion.getCliente();
        return new CotizacionResumenResponse(
            cotizacion.getId(),
            cliente == null ? null : cliente.getId(),
            cliente == null ? null : cliente.getNumeroDocumento(),
            nombreCliente(cliente),
            cotizacion.getUsuario().getId(),
            cotizacion.getUsuario().getUsuarioLogin(),
            cotizacion.getFecha(),
            cotizacion.getFechaVencimiento(),
            cotizacion.getSubtotal(),
            cotizacion.getIgv(),
            cotizacion.getTotal(),
            cotizacion.getEstado()
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
