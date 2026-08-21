package pe.com.proveperu.sgc.security.api.dto;

import jakarta.validation.constraints.NotNull;
import pe.com.proveperu.sgc.security.domain.model.EstadoUsuario;

public record UsuarioEstadoRequest(
    @NotNull(message = "El estado es obligatorio")
    EstadoUsuario estado
) {
}
