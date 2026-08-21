package pe.com.proveperu.sgc.catalogo.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record PrecioCrearRequest(
    @NotBlank(message = "El tipo de precio es obligatorio")
    @Size(max = 30, message = "El tipo de precio no puede superar 30 caracteres")
    @Pattern(
        regexp = "^[A-Za-z][A-Za-z0-9_-]{0,29}$",
        message = "El tipo de precio solo admite letras, números, guion y guion bajo"
    )
    String tipoPrecio,

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero")
    @Digits(integer = 12, fraction = 2, message = "El monto debe tener hasta 12 enteros y 2 decimales")
    BigDecimal monto,

    @NotNull(message = "La fecha de inicio es obligatoria")
    LocalDate vigenteDesde,

    LocalDate vigenteHasta
) {
    @AssertTrue(message = "La fecha final no puede ser anterior a la fecha inicial")
    public boolean isVigenciaValida() {
        return vigenteDesde == null
            || vigenteHasta == null
            || !vigenteHasta.isBefore(vigenteDesde);
    }
}
