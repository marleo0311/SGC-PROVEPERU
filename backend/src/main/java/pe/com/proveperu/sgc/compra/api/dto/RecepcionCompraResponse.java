package pe.com.proveperu.sgc.compra.api.dto;

import java.time.Instant;
import java.util.List;
import pe.com.proveperu.sgc.compra.domain.model.EstadoRecepcionCompra;
import pe.com.proveperu.sgc.compra.domain.model.RecepcionCompra;

public record RecepcionCompraResponse(
    Long id,
    Long idCompra,
    Long idSede,
    String sede,
    Long idUsuario,
    String usuarioLogin,
    Instant fechaHora,
    String observacion,
    EstadoRecepcionCompra estado,
    List<DetalleRecepcionCompraResponse> items
) {
    public static RecepcionCompraResponse from(RecepcionCompra recepcion) {
        return new RecepcionCompraResponse(
            recepcion.getId(),
            recepcion.getCompra().getId(),
            recepcion.getSede().getId(),
            recepcion.getSede().getNombre(),
            recepcion.getUsuario().getId(),
            recepcion.getUsuario().getUsuarioLogin(),
            recepcion.getFechaHora(),
            recepcion.getObservacion(),
            recepcion.getEstado(),
            recepcion.getDetalles().stream()
                .map(DetalleRecepcionCompraResponse::from)
                .toList()
        );
    }
}
