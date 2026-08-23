package pe.com.proveperu.sgc.facturacionelectronica.api.dto;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.AmbienteSunat;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.EnvioSunat;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.EstadoEnvioSunat;

public record EnvioSunatResponse(
    Long id,
    AmbienteSunat ambiente,
    EstadoEnvioSunat estado,
    String nombreArchivo,
    String hashXml,
    String ticket,
    String codigoRespuesta,
    String descripcionRespuesta,
    List<String> observaciones,
    String errorUltimo,
    int intentos,
    Instant fechaGeneracion,
    Instant fechaUltimoIntento,
    Instant fechaRespuesta,
    boolean xmlDisponible,
    boolean cdrDisponible
) {
    public static EnvioSunatResponse from(EnvioSunat envio) {
        return new EnvioSunatResponse(
            envio.getId(),
            envio.getAmbiente(),
            envio.getEstado(),
            envio.getNombreArchivo(),
            envio.getHashXml(),
            envio.getTicket(),
            envio.getCodigoRespuesta(),
            envio.getDescripcionRespuesta(),
            lineas(envio.getObservaciones()),
            envio.getErrorUltimo(),
            envio.getIntentos(),
            envio.getFechaGeneracion(),
            envio.getFechaUltimoIntento(),
            envio.getFechaRespuesta(),
            envio.getXmlFirmado() != null && envio.getXmlFirmado().length > 0,
            envio.getCdrZip() != null && envio.getCdrZip().length > 0
        );
    }

    private static List<String> lineas(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("\\R"))
            .filter(item -> !item.isBlank())
            .toList();
    }
}
