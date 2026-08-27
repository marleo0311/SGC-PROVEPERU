package pe.com.proveperu.sgc.inventario.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import pe.com.proveperu.sgc.catalogo.domain.model.Producto;
import pe.com.proveperu.sgc.inventario.domain.model.EstadoStock;
import pe.com.proveperu.sgc.inventario.domain.model.Inventario;
import pe.com.proveperu.sgc.inventario.domain.model.Sede;

public record StockInventarioResponse(
    Long idInventario,
    Long idSede,
    String nombreSede,
    Long idProducto,
    String codigoInterno,
    String codigoBarras,
    String nombreProducto,
    Long idUnidadBase,
    String codigoUnidadBase,
    String nombreUnidadBase,
    BigDecimal stockFisico,
    BigDecimal stockReservado,
    BigDecimal stockDisponible,
    BigDecimal stockMinimo,
    EstadoStock estadoStock,
    Instant fechaActualizacion
) {
    public static StockInventarioResponse from(
        Sede sede,
        Producto producto,
        Inventario inventario
    ) {
        BigDecimal stockFisico = inventario == null
            ? BigDecimal.ZERO.setScale(3)
            : inventario.getStockFisico();
        BigDecimal stockReservado = inventario == null
            ? BigDecimal.ZERO.setScale(3)
            : inventario.getStockReservado();
        BigDecimal stockDisponible = stockFisico.subtract(stockReservado);
        BigDecimal stockMinimo = inventario == null
            ? producto.getStockMinimo()
            : inventario.getStockMinimo();
        EstadoStock estadoStock = calcularEstado(stockDisponible, stockMinimo);

        return new StockInventarioResponse(
            inventario == null ? null : inventario.getId(),
            sede.getId(),
            sede.getNombre(),
            producto.getId(),
            producto.getCodigoInterno(),
            producto.getCodigoBarras(),
            producto.getNombre(),
            producto.getUnidadBase().getId(),
            producto.getUnidadBase().getCodigo(),
            producto.getUnidadBase().getNombre(),
            stockFisico,
            stockReservado,
            stockDisponible,
            stockMinimo,
            estadoStock,
            inventario == null ? null : inventario.getFechaActualizacion()
        );
    }

    private static EstadoStock calcularEstado(BigDecimal disponible, BigDecimal minimo) {
        if (disponible.compareTo(BigDecimal.ZERO) <= 0) {
            return EstadoStock.AGOTADO;
        }
        if (disponible.compareTo(minimo) <= 0) {
            return EstadoStock.BAJO;
        }
        return EstadoStock.NORMAL;
    }
}
