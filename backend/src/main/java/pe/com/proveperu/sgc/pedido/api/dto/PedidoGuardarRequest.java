package pe.com.proveperu.sgc.pedido.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import pe.com.proveperu.sgc.pedido.domain.model.CanalPedido;

public record PedidoGuardarRequest(
    @Positive(message = "El cliente debe ser válido")
    Long idCliente,

    @Positive(message = "La sede debe ser válida")
    Long idSede,

    @NotNull(message = "El canal es obligatorio")
    CanalPedido canal,

    @NotNull(message = "El IGV es obligatorio")
    @DecimalMin(value = "0.00", message = "El IGV no puede ser negativo")
    @Digits(integer = 12, fraction = 2, message = "El IGV admite hasta 12 enteros y 2 decimales")
    BigDecimal igv,

    @Size(max = 300, message = "La observación no puede superar 300 caracteres")
    String observacion,

    @NotEmpty(message = "El pedido debe contener al menos un producto")
    @Size(max = 200, message = "El pedido no puede superar 200 productos")
    List<@Valid PedidoDetalleRequest> detalles
) {
}
