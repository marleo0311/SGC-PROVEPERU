package pe.com.proveperu.sgc.reporte.api.dto;

import java.time.Instant;
import pe.com.proveperu.sgc.reporte.api.dto.ReporteCajaResponse.ResumenCajaResponse;
import pe.com.proveperu.sgc.reporte.api.dto.ReporteFinanzasResponse.SaldoPendienteResponse;
import pe.com.proveperu.sgc.reporte.api.dto.ReporteInventarioResponse.ResumenInventarioResponse;
import pe.com.proveperu.sgc.reporte.api.dto.ReporteVentasResponse.ResumenVentasResponse;

public record ReporteDashboardResponse(
    Instant fechaGeneracion,
    PeriodoReporteResponse periodo,
    ResumenVentasResponse ventas,
    ResumenInventarioResponse inventario,
    SaldoPendienteResponse cuentasCobrar,
    SaldoPendienteResponse cuentasPagar,
    ResumenCajaResponse caja
) {
}
