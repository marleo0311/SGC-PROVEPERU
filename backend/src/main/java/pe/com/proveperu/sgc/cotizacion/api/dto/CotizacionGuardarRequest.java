package pe.com.proveperu.sgc.cotizacion.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CotizacionGuardarRequest(
    @Positive(message = "El cliente debe ser válido")
    Long idCliente,

    @NotNull(message = "La fecha es obligatoria")
    LocalDate fecha,

    LocalDate fechaVencimiento,

    @NotNull(message = "Debe indicar si el precio incluye IGV")
    Boolean aplicarIgv,

    @NotEmpty(message = "La cotización debe contener al menos un producto")
    @Size(max = 200, message = "La cotización no puede superar 200 productos")
    List<@Valid CotizacionDetalleRequest> detalles
) {
    @AssertTrue(message = "El vencimiento no puede ser anterior a la fecha de la cotización")
    public boolean isVigenciaValida() {
        return fecha == null
            || fechaVencimiento == null
            || !fechaVencimiento.isBefore(fecha);
    }
}
