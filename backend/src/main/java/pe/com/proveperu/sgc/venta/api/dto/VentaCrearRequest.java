package pe.com.proveperu.sgc.venta.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import pe.com.proveperu.sgc.venta.domain.model.CondicionPagoVenta;
import pe.com.proveperu.sgc.venta.domain.model.TipoComprobanteVenta;
import pe.com.proveperu.sgc.venta.domain.model.TipoVenta;

public record VentaCrearRequest(
    @Positive(message = "El cliente debe ser válido")
    Long idCliente,

    @Positive(message = "El pedido debe ser válido")
    @Schema(description = "Pedido confirmado de origen; se omite para una venta directa")
    Long idPedido,

    @Positive(message = "La sede debe ser válida")
    @Schema(description = "Sede de una venta directa; si se omite usa la primera sede activa")
    Long idSede,

    @NotNull(message = "El tipo de venta es obligatorio")
    TipoVenta tipoVenta,

    @NotNull(message = "La condición de pago es obligatoria")
    CondicionPagoVenta condicionPago,

    @Positive(message = "El método de pago debe ser válido")
    Long idMetodoPago,

    @NotNull(message = "El tipo de comprobante es obligatorio")
    TipoComprobanteVenta tipoComprobante,

    @Schema(description = "Indica si el precio final incluye IGV. Si se omite en una venta directa, se considera verdadero; en pedidos se conserva su configuración")
    Boolean aplicarIgv,

    @DecimalMin(value = "0.01", message = "El pago inicial debe ser mayor que cero")
    @Digits(integer = 12, fraction = 2, message = "El pago inicial admite hasta 12 enteros y 2 decimales")
    @Schema(description = "Obligatorio solo para condición PARCIAL")
    BigDecimal montoPagado,

    @FutureOrPresent(message = "La fecha de vencimiento no puede estar en el pasado")
    LocalDate fechaVencimiento,

    @Size(max = 120, message = "La referencia no puede superar 120 caracteres")
    String referenciaPago,

    @Size(max = 200, message = "La venta no puede superar 200 productos")
    List<@Valid VentaItemRequest> items
) {
}
