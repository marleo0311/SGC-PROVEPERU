package pe.com.proveperu.sgc.reporte.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record ReporteInventarioResponse(
    Long idSede,
    String nombreSede,
    ResumenInventarioResponse resumen,
    List<ProductoStockBajoResponse> productosStockBajo
) {
    public record ResumenInventarioResponse(
        long productosActivos,
        long productosStockBajo,
        long productosAgotados
    ) {
    }

    public record ProductoStockBajoResponse(
        Long idProducto,
        String codigoInterno,
        String nombreProducto,
        String unidadBase,
        BigDecimal stockFisico,
        BigDecimal stockReservado,
        BigDecimal stockDisponible,
        BigDecimal stockMinimo,
        String estadoStock
    ) {
    }
}
