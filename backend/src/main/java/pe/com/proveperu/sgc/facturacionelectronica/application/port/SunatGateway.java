package pe.com.proveperu.sgc.facturacionelectronica.application.port;

public interface SunatGateway {

    byte[] enviarComprobante(String ruc, String nombreZip, byte[] contenidoZip);
}
