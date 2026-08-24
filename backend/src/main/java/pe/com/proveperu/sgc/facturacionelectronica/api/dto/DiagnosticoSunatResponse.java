package pe.com.proveperu.sgc.facturacionelectronica.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.AmbienteSunat;

public record DiagnosticoSunatResponse(
    Instant generadoEn,
    AmbienteSunat ambiente,
    boolean listoParaPiloto,
    boolean emisionRealHabilitada,
    int aprobados,
    int advertencias,
    int bloqueos,
    List<Verificacion> verificaciones,
    Certificado certificado,
    List<Serie> series
) {

    public enum EstadoVerificacion {
        APROBADO,
        ADVERTENCIA,
        BLOQUEO
    }

    public record Verificacion(
        String codigo,
        String nombre,
        EstadoVerificacion estado,
        String detalle,
        String accion
    ) {
    }

    public record Certificado(
        boolean configurado,
        boolean valido,
        boolean contieneClavePrivada,
        boolean rucCoincide,
        String titular,
        String emisor,
        LocalDate validoDesde,
        LocalDate validoHasta
    ) {
    }

    public record Serie(
        String tipoDocumento,
        String serie,
        long ultimoCorrelativo,
        String siguienteNumero,
        boolean activa
    ) {
    }
}
