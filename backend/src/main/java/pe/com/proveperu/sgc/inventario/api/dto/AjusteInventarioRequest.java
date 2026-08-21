package pe.com.proveperu.sgc.inventario.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import pe.com.proveperu.sgc.inventario.domain.model.TipoAjusteInventario;

public record AjusteInventarioRequest(
    @Positive(message = "La sede debe ser válida")
    Long idSede,

    @NotNull(message = "El producto es obligatorio")
    @Positive(message = "El producto debe ser válido")
    Long idProducto,

    @NotNull(message = "La unidad de medida es obligatoria")
    @Positive(message = "La unidad de medida debe ser válida")
    Long idUnidadMedida,

    @NotNull(message = "El tipo de ajuste es obligatorio")
    TipoAjusteInventario tipoAjuste,

    @NotNull(message = "La cantidad es obligatoria")
    @DecimalMin(value = "0.001", message = "La cantidad debe ser mayor que cero")
    @Digits(integer = 11, fraction = 3, message = "La cantidad admite hasta 11 enteros y 3 decimales")
    BigDecimal cantidad,

    @NotBlank(message = "El motivo es obligatorio")
    @Size(max = 250, message = "El motivo no puede superar 250 caracteres")
    String motivo
) {
}
