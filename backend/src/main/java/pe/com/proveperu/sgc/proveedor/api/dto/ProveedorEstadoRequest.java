package pe.com.proveperu.sgc.proveedor.api.dto;

import jakarta.validation.constraints.NotNull;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;

public record ProveedorEstadoRequest(
    @NotNull(message = "El estado es obligatorio")
    EstadoCatalogo estado
) {
}
