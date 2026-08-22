package pe.com.proveperu.sgc.reporte.api.dto;

import java.math.BigDecimal;

public record ReporteFinanzasResponse(
    SaldoPendienteResponse cuentasCobrar,
    SaldoPendienteResponse cuentasPagar,
    BigDecimal balancePendiente
) {
    public record SaldoPendienteResponse(
        long cantidadCuentas,
        BigDecimal saldoPendiente,
        long cantidadVencidas,
        BigDecimal saldoVencido
    ) {
    }
}
