package pe.com.proveperu.sgc.proveedor.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProveedorCompraResponse(
    Long idCompra,
    String tipoComprobante,
    String numeroComprobante,
    LocalDate fecha,
    String estado,
    BigDecimal importeTotal,
    BigDecimal saldoPendiente
) {
}
