package pe.com.proveperu.sgc.catalogo.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ConversionCrearRequest(
    @NotNull(message = "La unidad de origen es obligatoria")
    @Positive(message = "La unidad de origen debe ser válida")
    Long idUnidadOrigen,

    @NotNull(message = "La unidad de destino es obligatoria")
    @Positive(message = "La unidad de destino debe ser válida")
    Long idUnidadDestino,

    @NotNull(message = "El factor de conversión es obligatorio")
    @DecimalMin(value = "0.000001", message = "El factor de conversión debe ser mayor que cero")
    @Digits(integer = 12, fraction = 6, message = "El factor debe tener hasta 12 enteros y 6 decimales")
    BigDecimal factorConversion
) {
    @AssertTrue(message = "Las unidades de origen y destino deben ser diferentes")
    public boolean isUnidadesDiferentes() {
        return idUnidadOrigen == null
            || idUnidadDestino == null
            || !idUnidadOrigen.equals(idUnidadDestino);
    }
}
