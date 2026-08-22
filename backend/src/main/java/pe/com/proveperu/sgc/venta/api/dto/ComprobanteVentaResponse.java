package pe.com.proveperu.sgc.venta.api.dto;

import java.time.Instant;
import java.util.List;
import pe.com.proveperu.sgc.venta.domain.model.TipoComprobanteVenta;

public record ComprobanteVentaResponse(
    TipoComprobanteVenta tipo,
    String numero,
    Instant fechaEmision,
    VentaResumenResponse venta,
    List<VentaDetalleResponse> items
) {
}
