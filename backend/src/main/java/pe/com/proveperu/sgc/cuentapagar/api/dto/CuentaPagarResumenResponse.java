package pe.com.proveperu.sgc.cuentapagar.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import pe.com.proveperu.sgc.compra.domain.model.CondicionPagoCompra;
import pe.com.proveperu.sgc.cuentapagar.domain.model.CuentaPagar;
import pe.com.proveperu.sgc.cuentapagar.domain.model.EstadoCuentaPagar;

public record CuentaPagarResumenResponse(
    Long id,
    Long idCompra,
    Long idProveedor,
    String proveedorRuc,
    String proveedorRazonSocial,
    LocalDate fechaCompra,
    String tipoComprobante,
    String numeroComprobante,
    CondicionPagoCompra condicionPago,
    BigDecimal total,
    BigDecimal importePagado,
    BigDecimal saldoPendiente,
    LocalDate fechaVencimiento,
    EstadoCuentaPagar estado
) {
    public static CuentaPagarResumenResponse from(CuentaPagar cuenta) {
        var compra = cuenta.getCompra();
        var proveedor = compra.getProveedor();
        return new CuentaPagarResumenResponse(
            cuenta.getId(),
            compra.getId(),
            proveedor.getId(),
            proveedor.getRuc(),
            proveedor.getRazonSocial(),
            compra.getFecha(),
            compra.getTipoComprobante(),
            compra.getNumeroComprobante(),
            compra.getCondicionPago(),
            cuenta.getTotal(),
            cuenta.getImportePagado(),
            cuenta.getSaldoPendiente(),
            cuenta.getFechaVencimiento(),
            cuenta.getEstado()
        );
    }
}
