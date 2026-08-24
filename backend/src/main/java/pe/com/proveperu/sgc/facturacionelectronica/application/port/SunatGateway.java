package pe.com.proveperu.sgc.facturacionelectronica.application.port;

import pe.com.proveperu.sgc.facturacionelectronica.application.dto.EstadoTicketSunat;

public interface SunatGateway {

    byte[] enviarComprobante(String ruc, String nombreZip, byte[] contenidoZip);

    String enviarResumen(String ruc, String nombreZip, byte[] contenidoZip);

    EstadoTicketSunat consultarTicket(String ruc, String ticket);
}
