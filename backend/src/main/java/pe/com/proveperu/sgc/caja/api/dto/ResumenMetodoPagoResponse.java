package pe.com.proveperu.sgc.caja.api.dto;

import java.math.BigDecimal;

public record ResumenMetodoPagoResponse(
    Long idMetodoPago,
    String codigo,
    String nombre,
    BigDecimal ingresos,
    BigDecimal egresos,
    BigDecimal neto
) {
}
