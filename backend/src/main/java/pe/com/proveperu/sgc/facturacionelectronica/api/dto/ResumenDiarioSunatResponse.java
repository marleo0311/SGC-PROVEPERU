package pe.com.proveperu.sgc.facturacionelectronica.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import pe.com.proveperu.sgc.comprobante.domain.model.Comprobante;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.AmbienteSunat;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.EstadoResumenDiarioSunat;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.ResumenDiarioSunat;

public record ResumenDiarioSunatResponse(
    Long id,
    AmbienteSunat ambiente,
    LocalDate fechaDocumentos,
    LocalDate fechaGeneracion,
    int correlativo,
    EstadoResumenDiarioSunat estado,
    String nombreArchivo,
    String hashXml,
    String ticket,
    String codigoEstadoTicket,
    String codigoRespuesta,
    String descripcionRespuesta,
    List<String> observaciones,
    String errorUltimo,
    int intentosEnvio,
    int consultasEstado,
    Instant fechaCreacion,
    Instant fechaUltimoIntento,
    Instant fechaUltimaConsulta,
    Instant fechaRespuesta,
    BigDecimal total,
    List<BoletaResumenResponse> boletas,
    boolean xmlDisponible,
    boolean cdrDisponible
) {
    public static ResumenDiarioSunatResponse from(ResumenDiarioSunat resumen) {
        List<BoletaResumenResponse> boletas = resumen.getComprobantes().stream()
            .sorted(Comparator.comparing(Comprobante::getFechaEmision).thenComparing(Comprobante::getId))
            .map(BoletaResumenResponse::from)
            .toList();
        BigDecimal total = boletas.stream()
            .map(BoletaResumenResponse::total)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ResumenDiarioSunatResponse(
            resumen.getId(),
            resumen.getAmbiente(),
            resumen.getFechaDocumentos(),
            resumen.getFechaGeneracion(),
            resumen.getCorrelativo(),
            resumen.getEstado(),
            resumen.getNombreArchivo(),
            resumen.getHashXml(),
            resumen.getTicket(),
            resumen.getCodigoEstadoTicket(),
            resumen.getCodigoRespuesta(),
            resumen.getDescripcionRespuesta(),
            lineas(resumen.getObservaciones()),
            resumen.getErrorUltimo(),
            resumen.getIntentosEnvio(),
            resumen.getConsultasEstado(),
            resumen.getFechaCreacion(),
            resumen.getFechaUltimoIntento(),
            resumen.getFechaUltimaConsulta(),
            resumen.getFechaRespuesta(),
            total,
            boletas,
            resumen.getXmlFirmado() != null && resumen.getXmlFirmado().length > 0,
            resumen.getCdrZip() != null && resumen.getCdrZip().length > 0
        );
    }

    private static List<String> lineas(String value) {
        return value == null || value.isBlank()
            ? List.of()
            : Arrays.stream(value.split("\\R")).filter(line -> !line.isBlank()).toList();
    }

    public record BoletaResumenResponse(
        Long id,
        String numero,
        Instant fechaEmision,
        BigDecimal total
    ) {
        static BoletaResumenResponse from(Comprobante comprobante) {
            return new BoletaResumenResponse(
                comprobante.getId(),
                comprobante.getNumeroCompleto(),
                comprobante.getFechaEmision(),
                comprobante.getTotal()
            );
        }
    }
}
