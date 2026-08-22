package pe.com.proveperu.sgc.cuentacobrar.api.dto;

import java.util.List;
import pe.com.proveperu.sgc.venta.api.dto.PagoClienteResponse;

public record CuentaCobrarDetalleResponse(
    CuentaCobrarResumenResponse cuenta,
    List<PagoClienteResponse> pagos
) {
}
