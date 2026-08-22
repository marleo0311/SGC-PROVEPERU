package pe.com.proveperu.sgc.cuentapagar.api.dto;

import java.util.List;

public record CuentaPagarDetalleResponse(
    CuentaPagarResumenResponse cuenta,
    List<PagoProveedorResponse> pagos
) {
}
