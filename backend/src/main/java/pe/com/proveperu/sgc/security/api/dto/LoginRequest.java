package pe.com.proveperu.sgc.security.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "El usuario es obligatorio")
    @Size(max = 180, message = "El usuario no puede superar 180 caracteres")
    String usuarioLogin,

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(max = 200, message = "La contraseña no puede superar 200 caracteres")
    String password
) {
}
