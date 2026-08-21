package pe.com.proveperu.sgc.inventario.api.dto;

public record AjusteInventarioResponse(
    MovimientoInventarioResponse movimiento,
    StockInventarioResponse inventario
) {
}
