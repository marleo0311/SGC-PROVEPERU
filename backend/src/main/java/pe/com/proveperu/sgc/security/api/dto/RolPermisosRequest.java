package pe.com.proveperu.sgc.security.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record RolPermisosRequest(
    @NotNull(message = "La lista de permisos es obligatoria")
    Set<Long> idsPermisos
) {
}
