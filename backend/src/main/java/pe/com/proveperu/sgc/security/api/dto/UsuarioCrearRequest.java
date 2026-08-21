package pe.com.proveperu.sgc.security.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UsuarioCrearRequest(
    @NotBlank(message = "El nombre completo es obligatorio")
    @Size(max = 180, message = "El nombre completo no puede superar 180 caracteres")
    String nombreCompleto,

    @NotBlank(message = "El usuario es obligatorio")
    @Size(max = 180, message = "El usuario no puede superar 180 caracteres")
    String usuarioLogin,

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 12, max = 200, message = "La contraseña debe tener entre 12 y 200 caracteres")
    String password,

    @NotNull(message = "El rol es obligatorio")
    @Positive(message = "El rol debe ser válido")
    Long idRol
) {
}
