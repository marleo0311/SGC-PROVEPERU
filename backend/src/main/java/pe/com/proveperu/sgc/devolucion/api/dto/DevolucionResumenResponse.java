package pe.com.proveperu.sgc.devolucion.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import pe.com.proveperu.sgc.cliente.domain.model.Cliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoPersona;
import pe.com.proveperu.sgc.devolucion.domain.model.Devolucion;
import pe.com.proveperu.sgc.devolucion.domain.model.EstadoDevolucion;
import pe.com.proveperu.sgc.devolucion.domain.model.TipoSolucionDevolucion;

public record DevolucionResumenResponse(
    Long id,
    Long idVenta,
    String numeroComprobante,
    Long idCliente,
    String cliente,
    Long idUsuario,
    String usuarioLogin,
    Instant fechaHora,
    String motivo,
    TipoSolucionDevolucion tipoSolucion,
    EstadoDevolucion estado,
    BigDecimal importeTotal,
    BigDecimal importeAplicadoSaldo,
    BigDecimal importeReembolsable,
    BigDecimal importeReembolsado,
    BigDecimal importeReemplazo,
    BigDecimal importeCobrado
) {
    public static DevolucionResumenResponse from(Devolucion devolucion) {
        Long idCliente = devolucion.getVenta().getCliente() == null
            ? null
            : devolucion.getVenta().getCliente().getId();
        String cliente = nombreCliente(devolucion.getVenta().getCliente());
        Long idVenta = devolucion.getVenta().getId();
        return new DevolucionResumenResponse(
            devolucion.getId(),
            idVenta,
            "NV-%08d".formatted(idVenta),
            idCliente,
            cliente,
            devolucion.getUsuario().getId(),
            devolucion.getUsuario().getUsuarioLogin(),
            devolucion.getFechaHora(),
            devolucion.getMotivo(),
            devolucion.getTipoSolucion(),
            devolucion.getEstado(),
            devolucion.getImporteTotal(),
            devolucion.getImporteAplicadoSaldo(),
            devolucion.getImporteReembolsable(),
            devolucion.getImporteReembolsado(),
            devolucion.getImporteReemplazo(),
            devolucion.getImporteCobrado()
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
