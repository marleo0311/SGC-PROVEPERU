package pe.com.proveperu.sgc.comprobante.api.dto;

public record RepresentacionComprobanteResponse(
    String titulo,
    String moneda,
    EmpresaComprobanteResponse emisor,
    ClienteComprobanteResponse cliente,
    ComprobanteResponse comprobante,
    String nota
) {
}
