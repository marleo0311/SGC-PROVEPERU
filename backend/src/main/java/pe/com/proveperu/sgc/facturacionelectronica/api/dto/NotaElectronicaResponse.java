package pe.com.proveperu.sgc.facturacionelectronica.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.AmbienteSunat;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.EstadoEnvioSunat;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.NotaElectronica;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.TipoNotaElectronica;

public record NotaElectronicaResponse(
    Long id,
    Long idComprobanteOrigen,
    String comprobanteOrigen,
    TipoNotaElectronica tipo,
    String numeroCompleto,
    String codigoMotivo,
    String descripcionMotivo,
    Instant fechaEmision,
    BigDecimal subtotal,
    BigDecimal igv,
    BigDecimal total,
    String usuarioLogin,
    AmbienteSunat ambiente,
    EstadoEnvioSunat estado,
    String nombreArchivo,
    String codigoRespuesta,
    String descripcionRespuesta,
    List<String> observaciones,
    String errorUltimo,
    int intentos,
    Instant fechaUltimoIntento,
    Instant fechaRespuesta,
    boolean xmlDisponible,
    boolean cdrDisponible
) {
    public static NotaElectronicaResponse from(NotaElectronica nota) {
        List<String> observations = nota.getObservaciones() == null || nota.getObservaciones().isBlank()
            ? List.of()
            : nota.getObservaciones().lines().filter(line -> !line.isBlank()).toList();
        return new NotaElectronicaResponse(
            nota.getId(), nota.getComprobanteOrigen().getId(), nota.getComprobanteOrigen().getNumeroCompleto(),
            nota.getTipo(), nota.getNumeroCompleto(), nota.getCodigoMotivo(), nota.getDescripcionMotivo(),
            nota.getFechaEmision(), nota.getSubtotal(), nota.getIgv(), nota.getTotal(), nota.getUsuario().getUsuarioLogin(),
            nota.getAmbiente(), nota.getEstado(), nota.getNombreArchivo(), nota.getCodigoRespuesta(),
            nota.getDescripcionRespuesta(), observations, nota.getErrorUltimo(), nota.getIntentos(),
            nota.getFechaUltimoIntento(), nota.getFechaRespuesta(), nota.getXmlFirmado() != null,
            nota.getCdrZip() != null && nota.getCdrZip().length > 0
        );
    }
}
