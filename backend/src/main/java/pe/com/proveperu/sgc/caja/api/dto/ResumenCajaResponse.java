package pe.com.proveperu.sgc.caja.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record ResumenCajaResponse(
    SesionCajaResponse sesion,
    BigDecimal totalIngresos,
    BigDecimal totalEgresos,
    BigDecimal neto,
    BigDecimal saldoEsperado,
    List<ResumenMetodoPagoResponse> metodosPago
) {
}
