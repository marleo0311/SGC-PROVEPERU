package pe.com.proveperu.sgc.cotizacion.api.dto;

import jakarta.validation.constraints.NotNull;
import pe.com.proveperu.sgc.cotizacion.domain.model.EstadoCotizacion;

public record CotizacionEstadoRequest(
    @NotNull(message = "El estado es obligatorio")
    EstadoCotizacion estado
) {
}
