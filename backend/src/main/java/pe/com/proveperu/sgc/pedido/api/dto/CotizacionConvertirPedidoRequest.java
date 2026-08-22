package pe.com.proveperu.sgc.pedido.api.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import pe.com.proveperu.sgc.pedido.domain.model.CanalPedido;

public record CotizacionConvertirPedidoRequest(
    @Positive(message = "La sede debe ser válida")
    Long idSede,

    CanalPedido canal,

    @Size(max = 300, message = "La observación no puede superar 300 caracteres")
    String observacion
) {
}
