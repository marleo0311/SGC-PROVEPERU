package pe.com.proveperu.sgc.devolucion.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import pe.com.proveperu.sgc.devolucion.domain.model.EstadoProductoDevuelto;

public record DevolucionItemRequest(
    @NotNull(message = "El detalle de venta es obligatorio")
    @Positive(message = "El detalle de venta no es válido")
    Long idDetalleVenta,

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.000", inclusive = false, message = "La cantidad debe ser mayor que cero")
    @Digits(integer = 11, fraction = 3, message = "La cantidad admite hasta 11 enteros y 3 decimales")
    BigDecimal cantidad,

    @NotNull(message = "El estado del producto es obligatorio")
    EstadoProductoDevuelto estadoProducto
) {
}
