package pe.com.proveperu.sgc.security.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record RolCrearRequest(
    @NotBlank(message = "El nombre del rol es obligatorio")
    @Size(max = 80, message = "El nombre del rol no puede superar 80 caracteres")
    String nombre,

    @Size(max = 250, message = "La descripción no puede superar 250 caracteres")
    String descripcion,

    @NotNull(message = "La lista de permisos es obligatoria")
    Set<Long> idsPermisos
) {
}
