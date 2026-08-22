package pe.com.proveperu.sgc.cliente.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ClientePrecioEspecialRequest(
    @NotNull(message = "El producto es obligatorio")
    @Positive(message = "El producto debe ser válido")
    Long idProducto,

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser mayor que cero")
    @Digits(integer = 12, fraction = 2, message = "El precio admite hasta 12 enteros y 2 decimales")
    BigDecimal precio,

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
