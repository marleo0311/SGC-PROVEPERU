package pe.com.proveperu.sgc.facturacionelectronica.domain.model;

public enum EstadoEnvioSunat {
    GENERADO,
    ENVIANDO,
    ACEPTADO,
    ACEPTADO_CON_OBSERVACIONES,
    RECHAZADO,
    ERROR_COMUNICACION;

    public boolean aceptado() {
        return this == ACEPTADO || this == ACEPTADO_CON_OBSERVACIONES;
    }
}
