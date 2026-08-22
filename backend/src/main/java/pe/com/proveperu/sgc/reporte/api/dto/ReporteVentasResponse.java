package pe.com.proveperu.sgc.reporte.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReporteVentasResponse(
    PeriodoReporteResponse periodo,
    ResumenVentasResponse resumen,
    List<VentaDiariaResponse> ventasDiarias,
    List<VentaVendedorResponse> ventasPorVendedor,
    List<ProductoVendidoResponse> productosMasVendidos
) {
    public record ResumenVentasResponse(
        long cantidadVentas,
        BigDecimal subtotal,
        BigDecimal igv,
        BigDecimal descuentos,
        BigDecimal totalVentas,
        BigDecimal ticketPromedio
    ) {
    }

    public record VentaDiariaResponse(
        LocalDate fecha,
        long cantidadVentas,
        BigDecimal totalVentas
    ) {
    }

    public record VentaVendedorResponse(
        Long idVendedor,
        String usuarioLogin,
        String nombreCompleto,
        long cantidadVentas,
        BigDecimal totalVentas
    ) {
    }

    public record ProductoVendidoResponse(
        Long idProducto,
        String codigoInterno,
        String nombreProducto,
        BigDecimal cantidadBaseVendida,
        BigDecimal subtotalVendido
    ) {
    }
}
