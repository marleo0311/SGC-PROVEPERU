package pe.com.proveperu.sgc.compra.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import pe.com.proveperu.sgc.compra.domain.model.CondicionPagoCompra;

public record CompraGuardarRequest(
    @NotNull(message = "El proveedor es obligatorio")
    @Positive(message = "El proveedor debe ser válido")
    Long idProveedor,

    @NotNull(message = "La fecha es obligatoria")
    LocalDate fecha,

    @Size(max = 30, message = "El tipo de comprobante no puede superar 30 caracteres")
    String tipoComprobante,

    @Size(max = 60, message = "El número de comprobante no puede superar 60 caracteres")
    String numeroComprobante,

    @NotNull(message = "La condición de pago es obligatoria")
    CondicionPagoCompra condicionPago,

    @NotNull(message = "El IGV es obligatorio")
    @DecimalMin(value = "0.00", message = "El IGV no puede ser negativo")
    @Digits(integer = 12, fraction = 2, message = "El IGV admite hasta 12 enteros y 2 decimales")
    BigDecimal igv,

    @NotEmpty(message = "La compra debe contener al menos un producto")
    @Size(max = 200, message = "La compra no puede superar 200 detalles")
    List<@Valid CompraDetalleRequest> detalles
) {
    @AssertTrue(message = "El tipo y el número de comprobante deben informarse juntos")
    public boolean isComprobanteCompleto() {
        boolean tieneTipo = tipoComprobante != null && !tipoComprobante.isBlank();
        boolean tieneNumero = numeroComprobante != null && !numeroComprobante.isBlank();
        return tieneTipo == tieneNumero;
    }
}
