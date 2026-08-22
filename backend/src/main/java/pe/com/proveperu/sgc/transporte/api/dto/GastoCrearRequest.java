package pe.com.proveperu.sgc.transporte.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import pe.com.proveperu.sgc.transporte.domain.model.TipoGasto;

public record GastoCrearRequest(
    @Positive(message = "El transportista debe ser válido")
    Long idTransportista,

    @NotNull(message = "El tipo de gasto es obligatorio")
    TipoGasto tipoGasto,

    @Size(max = 250, message = "La descripción no puede superar 250 caracteres")
    String descripcion,

    @NotNull(message = "El importe es obligatorio")
    @DecimalMin(value = "0.01", message = "El importe debe ser mayor que cero")
    @Digits(integer = 12, fraction = 2, message = "El importe admite hasta 12 enteros y 2 decimales")
    BigDecimal importe,

    @NotNull(message = "La fecha es obligatoria")
    LocalDate fecha,

    @Size(max = 60, message = "El comprobante no puede superar 60 caracteres")
    String numeroComprobante
) {
    @AssertTrue(message = "Un gasto de transporte requiere un transportista")
    public boolean isTransportistaRequerido() {
        return tipoGasto == null
            || tipoGasto != TipoGasto.TRANSPORTE
            || idTransportista != null;
    }
}
