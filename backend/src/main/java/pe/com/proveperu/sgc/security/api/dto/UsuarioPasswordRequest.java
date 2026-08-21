package pe.com.proveperu.sgc.security.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioPasswordRequest(
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 12, max = 200, message = "La contraseña debe tener entre 12 y 200 caracteres")
    String password
) {
}
