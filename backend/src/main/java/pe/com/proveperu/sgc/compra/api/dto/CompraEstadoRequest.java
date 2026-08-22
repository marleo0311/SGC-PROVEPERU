package pe.com.proveperu.sgc.compra.api.dto;

import jakarta.validation.constraints.NotNull;
import pe.com.proveperu.sgc.compra.domain.model.EstadoCompra;

public record CompraEstadoRequest(
    @NotNull(message = "El estado es obligatorio")
    EstadoCompra estado
) {
}
