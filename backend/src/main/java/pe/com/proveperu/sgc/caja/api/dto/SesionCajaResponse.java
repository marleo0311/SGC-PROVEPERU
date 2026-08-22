package pe.com.proveperu.sgc.caja.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import pe.com.proveperu.sgc.caja.domain.model.EstadoSesionCaja;
import pe.com.proveperu.sgc.caja.domain.model.SesionCaja;

public record SesionCajaResponse(
    Long id,
    CajaResponse caja,
    Long idUsuarioApertura,
    String usuarioApertura,
    Instant fechaHoraApertura,
    BigDecimal saldoInicial,
    Long idUsuarioCierre,
    String usuarioCierre,
    Instant fechaHoraCierre,
    BigDecimal saldoEsperado,
    BigDecimal saldoReal,
    BigDecimal diferencia,
    String observacionCierre,
    EstadoSesionCaja estado
) {
    public static SesionCajaResponse from(SesionCaja sesion) {
        Long idUsuarioCierre = sesion.getUsuarioCierre() == null
            ? null
            : sesion.getUsuarioCierre().getId();
        String usuarioCierre = sesion.getUsuarioCierre() == null
            ? null
            : sesion.getUsuarioCierre().getUsuarioLogin();
        return new SesionCajaResponse(
            sesion.getId(),
            CajaResponse.from(sesion.getCaja()),
            sesion.getUsuarioApertura().getId(),
            sesion.getUsuarioApertura().getUsuarioLogin(),
            sesion.getFechaHoraApertura(),
            sesion.getSaldoInicial(),
            idUsuarioCierre,
            usuarioCierre,
            sesion.getFechaHoraCierre(),
            sesion.getSaldoEsperado(),
            sesion.getSaldoReal(),
            sesion.getDiferencia(),
            sesion.getObservacionCierre(),
            sesion.getEstado()
        );
    }
}
