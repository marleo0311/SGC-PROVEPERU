package pe.com.proveperu.sgc.facturacionelectronica.application.dto;

public record EstadoTicketSunat(
    String codigo,
    String mensaje,
    byte[] contenido
) {
    public boolean procesando() {
        return "98".equals(codigo);
    }

    public boolean terminado() {
        return "0".equals(codigo) || "99".equals(codigo);
    }
}
