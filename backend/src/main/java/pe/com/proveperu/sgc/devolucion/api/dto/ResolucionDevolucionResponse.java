package pe.com.proveperu.sgc.devolucion.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import pe.com.proveperu.sgc.devolucion.domain.model.Devolucion;
import pe.com.proveperu.sgc.devolucion.domain.model.TipoSolucionDevolucion;

public record ResolucionDevolucionResponse(
    Long idUsuario,
    String usuarioLogin,
    Instant fechaHora,
    Long idMetodoPago,
    String metodoPagoCodigo,
    String metodoPagoNombre,
    String referencia,
    BigDecimal importeDescuento,
    BigDecimal importeReemplazo,
    BigDecimal importeCobrado,
    List<DetalleCambioDevolucionResponse> reemplazos
) {
    public static ResolucionDevolucionResponse from(Devolucion devolucion) {
        if (devolucion.getUsuarioResolucion() == null) {
            return null;
        }
        Long idMetodo = devolucion.getMetodoPagoResolucion() == null
            ? null
            : devolucion.getMetodoPagoResolucion().getId();
        String codigoMetodo = devolucion.getMetodoPagoResolucion() == null
            ? null
            : devolucion.getMetodoPagoResolucion().getCodigo();
        String nombreMetodo = devolucion.getMetodoPagoResolucion() == null
            ? null
            : devolucion.getMetodoPagoResolucion().getNombre();
        BigDecimal descuento = devolucion.getTipoSolucion()
            == TipoSolucionDevolucion.DESCUENTO
                ? devolucion.getImporteAplicadoSaldo()
                    .add(devolucion.getImporteReembolsable())
                : BigDecimal.ZERO.setScale(2);
        return new ResolucionDevolucionResponse(
            devolucion.getUsuarioResolucion().getId(),
            devolucion.getUsuarioResolucion().getUsuarioLogin(),
            devolucion.getFechaResolucion(),
            idMetodo,
            codigoMetodo,
            nombreMetodo,
            devolucion.getReferenciaResolucion(),
            descuento,
            devolucion.getImporteReemplazo(),
            devolucion.getImporteCobrado(),
            devolucion.getDetallesCambio().stream()
                .map(DetalleCambioDevolucionResponse::from)
                .toList()
        );
    }
}
