package pe.com.proveperu.sgc.catalogo.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PresentacionProductoGuardarRequest(
    @NotBlank(message = "El nombre de la presentación es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    String nombre,

    @NotNull(message = "La unidad de presentación es obligatoria")
    @Positive(message = "La unidad de presentación debe ser válida")
    Long idUnidadMedida,

    @NotNull(message = "Debe indicar si el contenido es variable")
    Boolean contenidoVariable,

    @DecimalMin(value = "0.001", message = "El contenido debe ser mayor que cero")
    @Digits(integer = 11, fraction = 3, message = "El contenido admite hasta 11 enteros y 3 decimales")
    BigDecimal contenidoBasePredeterminado
) {
    @AssertTrue(message = "Una presentación fija requiere contenido predeterminado; una variable se informa al recibir")
    public boolean isContenidoCoherente() {
        return contenidoVariable == null
            || (contenidoVariable && contenidoBasePredeterminado == null)
            || (!contenidoVariable && contenidoBasePredeterminado != null);
    }
}
