package pe.com.proveperu.sgc.transporte.api.dto;

import jakarta.validation.constraints.NotNull;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;

public record TransportistaEstadoRequest(
    @NotNull(message = "El estado es obligatorio")
    EstadoCatalogo estado
) {
}
