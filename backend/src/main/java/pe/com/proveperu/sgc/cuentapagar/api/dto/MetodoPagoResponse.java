package pe.com.proveperu.sgc.cuentapagar.api.dto;

import pe.com.proveperu.sgc.configuracion.domain.model.MetodoPago;

public record MetodoPagoResponse(
    Long id,
    String codigo,
    String nombre
) {
    public static MetodoPagoResponse from(MetodoPago metodoPago) {
        return new MetodoPagoResponse(
            metodoPago.getId(),
            metodoPago.getCodigo(),
            metodoPago.getNombre()
        );
    }
}
