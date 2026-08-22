package pe.com.proveperu.sgc.cliente.api.dto;

import jakarta.validation.constraints.NotNull;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;

public record ClienteEstadoRequest(
    @NotNull(message = "El estado es obligatorio")
    EstadoCatalogo estado
) {
}
