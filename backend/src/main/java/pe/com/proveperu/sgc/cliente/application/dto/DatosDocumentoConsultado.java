package pe.com.proveperu.sgc.cliente.application.dto;

import pe.com.proveperu.sgc.cliente.domain.model.TipoDocumentoCliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoPersona;

public record DatosDocumentoConsultado(
    TipoPersona tipoPersona,
    TipoDocumentoCliente tipoDocumento,
    String numeroDocumento,
    String nombres,
    String apellidos,
    String razonSocial,
    String nombreComercial,
    String direccion,
    String estadoContribuyente,
    String condicionDomicilio
) {
    public String nombreMostrar() {
        if (tipoPersona == TipoPersona.JURIDICA) {
            return razonSocial;
        }
        return ((nombres == null ? "" : nombres) + " " + (apellidos == null ? "" : apellidos))
            .strip();
    }
}
