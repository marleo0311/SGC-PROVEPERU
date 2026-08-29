package pe.com.proveperu.sgc.inventario.api.dto;

import java.util.List;

public record IngresoPresentacionesResponse(
    List<ExistenciaPresentacionResponse> presentaciones,
    MovimientoInventarioResponse movimiento,
    StockInventarioResponse inventario
) {
}
