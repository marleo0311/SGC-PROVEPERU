package pe.com.proveperu.sgc.cliente.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.com.proveperu.sgc.cliente.domain.model.TipoDocumentoCliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoPersona;

public record ClienteGuardarRequest(
    @NotNull(message = "El tipo de persona es obligatorio")
    TipoPersona tipoPersona,

    @NotNull(message = "El tipo de documento es obligatorio")
    TipoDocumentoCliente tipoDocumento,

    @NotBlank(message = "El número de documento es obligatorio")
    @Pattern(regexp = "^[0-9]{8}$|^[0-9]{11}$", message = "El documento debe tener 8 u 11 dígitos")
    String numeroDocumento,

    @Size(max = 120, message = "Los nombres no pueden superar 120 caracteres")
    String nombres,

    @Size(max = 120, message = "Los apellidos no pueden superar 120 caracteres")
    String apellidos,

    @Size(max = 200, message = "La razón social no puede superar 200 caracteres")
    String razonSocial,

    @Size(max = 180, message = "El nombre comercial no puede superar 180 caracteres")
    String nombreComercial,

    @Size(max = 250, message = "La dirección no puede superar 250 caracteres")
    String direccion,

    @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
    String telefono,

    @Size(max = 30, message = "El WhatsApp no puede superar 30 caracteres")
    String whatsapp,

    @Email(message = "El correo no tiene un formato válido")
    @Size(max = 180, message = "El correo no puede superar 180 caracteres")
    String correo,

    @NotNull(message = "Debe indicar si el cliente tiene crédito autorizado")
    Boolean permiteCredito
) {
    @AssertTrue(message = "Los datos no corresponden al tipo de persona seleccionado")
    public boolean isDatosTipoPersonaValidos() {
        if (tipoPersona == null || tipoDocumento == null || numeroDocumento == null) {
            return true;
        }
        if (tipoPersona == TipoPersona.NATURAL) {
            return tipoDocumento == TipoDocumentoCliente.DNI
                && numeroDocumento.matches("^[0-9]{8}$")
                && tieneTexto(nombres)
                && tieneTexto(apellidos)
                && !tieneTexto(razonSocial);
        }
        return tipoDocumento == TipoDocumentoCliente.RUC
            && numeroDocumento.matches("^[0-9]{11}$")
            && tieneTexto(razonSocial)
            && !tieneTexto(nombres)
            && !tieneTexto(apellidos);
    }

    private boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }
}
