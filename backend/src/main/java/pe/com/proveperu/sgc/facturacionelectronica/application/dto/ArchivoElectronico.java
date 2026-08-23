package pe.com.proveperu.sgc.facturacionelectronica.application.dto;

public record ArchivoElectronico(
    String nombre,
    String contentType,
    byte[] contenido
) {
}
