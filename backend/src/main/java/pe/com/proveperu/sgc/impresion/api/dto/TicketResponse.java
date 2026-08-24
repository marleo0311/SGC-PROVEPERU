package pe.com.proveperu.sgc.impresion.api.dto;

import java.time.Instant;
import pe.com.proveperu.sgc.comprobante.domain.model.EstadoComprobante;
import pe.com.proveperu.sgc.impresion.domain.model.FormatoTicket;

public record TicketResponse(
    Long idComprobante,
    Long idVenta,
    String numeroComprobante,
    EstadoComprobante estado,
    FormatoTicket formato,
    int anchoCaracteres,
    String codificacion,
    boolean incluyeComandosEscPos,
    Instant fechaGeneracion,
    String contenido,
    String qrContenido,
    String qrImagenPngBase64
) {
}
