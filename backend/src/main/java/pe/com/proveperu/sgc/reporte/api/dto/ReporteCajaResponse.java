package pe.com.proveperu.sgc.reporte.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReporteCajaResponse(
    PeriodoReporteResponse periodo,
    ResumenCajaResponse resumen,
    List<CajaMetodoPagoResponse> metodosPago
) {
    public record ResumenCajaResponse(
        long cantidadMovimientos,
        BigDecimal totalIngresos,
        BigDecimal totalEgresos,
        BigDecimal neto
    ) {
    }

    public record CajaMetodoPagoResponse(
        Long idMetodoPago,
        String codigo,
        String nombre,
        BigDecimal ingresos,
        BigDecimal egresos,
        BigDecimal neto
    ) {
    }
}
