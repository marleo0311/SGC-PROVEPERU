package pe.com.proveperu.sgc.proveedor.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ProveedorHistorialResponse(
    ProveedorResponse proveedor,
    Resumen resumen,
    List<ProveedorCompraResponse> compras
) {
    public record Resumen(
        long totalCompras,
        BigDecimal importeTotal,
        BigDecimal saldoPendiente,
        LocalDate ultimaCompra
    ) {
    }
}
