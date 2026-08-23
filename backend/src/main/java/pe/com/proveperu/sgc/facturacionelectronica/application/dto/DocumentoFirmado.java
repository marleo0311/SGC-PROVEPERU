package pe.com.proveperu.sgc.facturacionelectronica.application.dto;

public record DocumentoFirmado(
    String nombreBase,
    byte[] xmlFirmado,
    byte[] zip,
    String hashSha256
) {
    public String nombreXml() {
        return nombreBase + ".xml";
    }

    public String nombreZip() {
        return nombreBase + ".zip";
    }
}
