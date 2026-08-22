package pe.com.proveperu.sgc.transporte.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import pe.com.proveperu.sgc.transporte.domain.model.TipoDocumentoTransportista;

public record TransportistaGuardarRequest(
    TipoDocumentoTransportista tipoDocumento,

    @Pattern(
        regexp = "^[0-9]{8}$|^[0-9]{11}$",
        message = "El documento debe tener 8 u 11 dígitos"
    )
    String numeroDocumento,

    @NotBlank(message = "El nombre o razón social es obligatorio")
    @Size(max = 200, message = "El nombre o razón social no puede superar 200 caracteres")
    String nombreRazonSocial,

    @Size(max = 180, message = "La empresa de transporte no puede superar 180 caracteres")
    String empresaTransporte,

    @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
    String telefono,

    @Size(max = 250, message = "La dirección no puede superar 250 caracteres")
    String direccion
) {
    @AssertTrue(message = "El tipo y número de documento no son compatibles")
    public boolean isDocumentoValido() {
        boolean tieneNumero = numeroDocumento != null && !numeroDocumento.isBlank();
        if (tipoDocumento == null) {
            return !tieneNumero;
        }
        if (!tieneNumero) {
            return false;
        }
        return tipoDocumento == TipoDocumentoTransportista.DNI
            ? numeroDocumento.matches("^[0-9]{8}$")
            : numeroDocumento.matches("^[0-9]{11}$");
    }
}
