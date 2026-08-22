package pe.com.proveperu.sgc.cuentacobrar.api.dto;

import pe.com.proveperu.sgc.configuracion.domain.model.MetodoPago;

public record MetodoPagoCobranzaResponse(
    Long id,
    String codigo,
    String nombre
) {
    public static MetodoPagoCobranzaResponse from(MetodoPago metodoPago) {
        return new MetodoPagoCobranzaResponse(
            metodoPago.getId(),
            metodoPago.getCodigo(),
            metodoPago.getNombre()
        );
    }
}
