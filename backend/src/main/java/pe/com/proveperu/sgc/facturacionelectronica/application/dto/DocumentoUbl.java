package pe.com.proveperu.sgc.facturacionelectronica.application.dto;

public record DocumentoUbl(
    String nombreBase,
    byte[] xml
) {
    public String nombreXml() {
        return nombreBase + ".xml";
    }

    public String nombreZip() {
        return nombreBase + ".zip";
    }
}
