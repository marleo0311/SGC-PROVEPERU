package pe.com.proveperu.sgc.caja.api.dto;

import pe.com.proveperu.sgc.configuracion.domain.model.MetodoPago;

public record MetodoPagoCajaResponse(
    Long id,
    String codigo,
    String nombre
) {
    public static MetodoPagoCajaResponse from(MetodoPago metodoPago) {
        return new MetodoPagoCajaResponse(
            metodoPago.getId(),
            metodoPago.getCodigo(),
            metodoPago.getNombre()
        );
    }
}
