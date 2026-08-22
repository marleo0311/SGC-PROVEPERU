package pe.com.proveperu.sgc.venta.api.dto;

import java.util.List;

public record VentaResponse(
    VentaResumenResponse venta,
    List<VentaDetalleResponse> detalles,
    CuentaCobrarVentaResponse cuentaCobrar,
    List<PagoClienteResponse> pagos
) {
}
