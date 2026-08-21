package pe.com.proveperu.sgc.catalogo.api.dto;

import jakarta.validation.constraints.NotNull;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;

public record ProductoEstadoRequest(
    @NotNull(message = "El estado es obligatorio")
    EstadoCatalogo estado
) {
}
