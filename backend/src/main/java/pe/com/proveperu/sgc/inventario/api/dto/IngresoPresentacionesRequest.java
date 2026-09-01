package pe.com.proveperu.sgc.inventario.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record IngresoPresentacionesRequest(
    @NotNull(message = "El almacén es obligatorio") @Positive Long idSede,
    @NotNull(message = "El producto es obligatorio") @Positive Long idProducto,
    @NotNull(message = "La presentación es obligatoria") @Positive Long idPresentacionProducto,

    @Min(value = 1, message = "Debe registrar al menos un bulto")
    @Max(value = 200, message = "Solo se pueden registrar 200 bultos por operación")
    Integer cantidadBultos,

    @Size(max = 200, message = "Solo se pueden registrar 200 bultos por operación")
    List<@NotNull @DecimalMin("0.001") @Digits(integer = 11, fraction = 3) BigDecimal> contenidosBase,
    @NotNull(message = "El motivo es obligatorio")
    @Size(min = 3, max = 250, message = "El motivo debe tener entre 3 y 250 caracteres")
    String motivo
) {
    public IngresoPresentacionesRequest(
        Long idSede,
        Long idProducto,
        Long idPresentacionProducto,
        List<BigDecimal> contenidosBase,
        String motivo
    ) {
        this(
            idSede,
            idProducto,
            idPresentacionProducto,
            null,
            contenidosBase,
            motivo
        );
    }
}
