package pe.com.proveperu.sgc.comprobante.api.dto;

import pe.com.proveperu.sgc.cliente.domain.model.Cliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoDocumentoCliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoPersona;

public record ClienteComprobanteResponse(
    Long id,
    TipoDocumentoCliente tipoDocumento,
    String numeroDocumento,
    String nombre,
    String direccion
) {
    public static ClienteComprobanteResponse from(Cliente cliente) {
        if (cliente == null) {
            return new ClienteComprobanteResponse(
                null,
                null,
                null,
                "CONSUMIDOR FINAL",
                null
            );
        }
        String nombre = cliente.getTipoPersona() == TipoPersona.NATURAL
            ? (cliente.getNombres() + " " + cliente.getApellidos()).strip()
            : cliente.getRazonSocial();
        return new ClienteComprobanteResponse(
            cliente.getId(),
            cliente.getTipoDocumento(),
            cliente.getNumeroDocumento(),
            nombre,
            cliente.getDireccion()
        );
    }
}
