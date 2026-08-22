package pe.com.proveperu.sgc.proveedor.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProveedorGuardarRequest(
    @NotBlank(message = "El RUC es obligatorio")
    @Pattern(regexp = "^[0-9]{11}$", message = "El RUC debe tener exactamente 11 dígitos")
    String ruc,

    @NotBlank(message = "La razón social es obligatoria")
    @Size(max = 200, message = "La razón social no puede superar 200 caracteres")
    String razonSocial,

    @Size(max = 180, message = "El nombre comercial no puede superar 180 caracteres")
    String nombreComercial,

    @Size(max = 250, message = "La dirección no puede superar 250 caracteres")
    String direccion,

    @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
    String telefono,

    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 180, message = "El correo no puede superar 180 caracteres")
    String correo,

    @Size(max = 180, message = "La persona de contacto no puede superar 180 caracteres")
    String personaContacto
) {
}
