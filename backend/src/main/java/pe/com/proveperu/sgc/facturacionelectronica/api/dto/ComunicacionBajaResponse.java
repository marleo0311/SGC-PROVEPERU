package pe.com.proveperu.sgc.facturacionelectronica.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.AmbienteSunat;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.ComunicacionBajaSunat;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.EstadoResumenDiarioSunat;

public record ComunicacionBajaResponse(
    Long id,
    Long idComprobante,
    String comprobante,
    String canal,
    String motivo,
    AmbienteSunat ambiente,
    LocalDate fechaDocumento,
    LocalDate fechaGeneracion,
    EstadoResumenDiarioSunat estado,
    String nombreArchivo,
    String ticket,
    String codigoRespuesta,
    String descripcionRespuesta,
    List<String> observaciones,
    String errorUltimo,
    int intentosEnvio,
    int consultasEstado,
    Instant fechaRespuesta,
    boolean xmlDisponible,
    boolean cdrDisponible
) {
    public static ComunicacionBajaResponse from(ComunicacionBajaSunat baja) {
        List<String> observaciones = baja.getObservaciones() == null || baja.getObservaciones().isBlank()
            ? List.of()
            : baja.getObservaciones().lines().filter(line -> !line.isBlank()).toList();
        return new ComunicacionBajaResponse(
            baja.getId(), baja.getComprobante().getId(), baja.getComprobante().getNumeroCompleto(),
            "COMUNICACION_BAJA", baja.getMotivo(), baja.getAmbiente(), baja.getFechaDocumento(),
            baja.getFechaGeneracion(), baja.getEstado(), baja.getNombreArchivo(), baja.getTicket(),
            baja.getCodigoRespuesta(), baja.getDescripcionRespuesta(), observaciones,
            baja.getErrorUltimo(), baja.getIntentosEnvio(), baja.getConsultasEstado(),
            baja.getFechaRespuesta(), baja.getXmlFirmado() != null,
            baja.getCdrZip() != null && baja.getCdrZip().length > 0
        );
    }

    public static ComunicacionBajaResponse boleta(
        Long idComprobante,
        String comprobante,
        String motivo,
        AmbienteSunat ambiente,
        LocalDate fechaDocumento
    ) {
        return new ComunicacionBajaResponse(
            null, idComprobante, comprobante, "RESUMEN_DIARIO", motivo, ambiente,
            fechaDocumento, null, EstadoResumenDiarioSunat.GENERADO, null, null,
            null, "Pendiente de incluirse como anulación en el Resumen Diario", List.of(),
            null, 0, 0, null, false, false
        );
    }
}
