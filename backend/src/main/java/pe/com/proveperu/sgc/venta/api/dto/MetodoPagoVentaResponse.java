package pe.com.proveperu.sgc.venta.api.dto;

import pe.com.proveperu.sgc.configuracion.domain.model.MetodoPago;

public record MetodoPagoVentaResponse(
    Long id,
    String codigo,
    String nombre
) {
    public static MetodoPagoVentaResponse from(MetodoPago metodoPago) {
        return new MetodoPagoVentaResponse(
            metodoPago.getId(),
            metodoPago.getCodigo(),
            metodoPago.getNombre()
        );
    }
}
