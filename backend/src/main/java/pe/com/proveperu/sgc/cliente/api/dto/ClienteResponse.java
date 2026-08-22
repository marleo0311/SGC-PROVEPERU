package pe.com.proveperu.sgc.cliente.api.dto;

import java.time.Instant;
import pe.com.proveperu.sgc.cliente.domain.model.Cliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoDocumentoCliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoPersona;

public record ClienteResponse(
    Long id,
    TipoPersona tipoPersona,
    TipoDocumentoCliente tipoDocumento,
    String numeroDocumento,
    String nombres,
    String apellidos,
    String razonSocial,
    String nombreComercial,
    String nombreMostrar,
    String direccion,
    String telefono,
    String whatsapp,
    String correo,
    boolean permiteCredito,
    String estado,
    Instant fechaRegistro,
    Instant fechaActualizacion
) {
    public static ClienteResponse from(Cliente cliente) {
        String nombreMostrar = cliente.getTipoPersona() == TipoPersona.NATURAL
            ? cliente.getNombres() + " " + cliente.getApellidos()
            : cliente.getRazonSocial();
        return new ClienteResponse(
            cliente.getId(),
            cliente.getTipoPersona(),
            cliente.getTipoDocumento(),
            cliente.getNumeroDocumento(),
            cliente.getNombres(),
            cliente.getApellidos(),
            cliente.getRazonSocial(),
            cliente.getNombreComercial(),
            nombreMostrar,
            cliente.getDireccion(),
            cliente.getTelefono(),
            cliente.getWhatsapp(),
            cliente.getCorreo(),
            cliente.isPermiteCredito(),
            cliente.getEstado().name(),
            cliente.getFechaRegistro(),
            cliente.getFechaActualizacion()
        );
    }
}
