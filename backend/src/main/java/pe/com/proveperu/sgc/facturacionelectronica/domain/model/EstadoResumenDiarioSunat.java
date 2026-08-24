package pe.com.proveperu.sgc.facturacionelectronica.domain.model;

public enum EstadoResumenDiarioSunat {
    GENERADO,
    ENVIANDO,
    TICKET_RECIBIDO,
    PROCESANDO,
    ACEPTADO,
    ACEPTADO_CON_OBSERVACIONES,
    RECHAZADO,
    ERROR_COMUNICACION;

    public boolean aceptado() {
        return this == ACEPTADO || this == ACEPTADO_CON_OBSERVACIONES;
    }

    public boolean tieneTicketPendiente() {
        return this == TICKET_RECIBIDO || this == PROCESANDO;
    }
}
