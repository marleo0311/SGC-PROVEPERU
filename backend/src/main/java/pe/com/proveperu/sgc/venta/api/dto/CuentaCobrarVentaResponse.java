package pe.com.proveperu.sgc.venta.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import pe.com.proveperu.sgc.venta.domain.model.CuentaCobrar;
import pe.com.proveperu.sgc.venta.domain.model.EstadoCuentaCobrar;

public record CuentaCobrarVentaResponse(
    Long id,
    BigDecimal total,
    BigDecimal importePagado,
    BigDecimal saldoPendiente,
    LocalDate fechaVencimiento,
    EstadoCuentaCobrar estado
) {
    public static CuentaCobrarVentaResponse from(CuentaCobrar cuenta) {
        return cuenta == null ? null : new CuentaCobrarVentaResponse(
            cuenta.getId(),
            cuenta.getTotal(),
            cuenta.getImportePagado(),
            cuenta.getSaldoPendiente(),
            cuenta.getFechaVencimiento(),
            cuenta.getEstado()
        );
    }
}
