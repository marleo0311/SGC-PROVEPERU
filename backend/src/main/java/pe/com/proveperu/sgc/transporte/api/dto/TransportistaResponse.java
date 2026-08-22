package pe.com.proveperu.sgc.transporte.api.dto;

import java.time.Instant;
import pe.com.proveperu.sgc.transporte.domain.model.TipoDocumentoTransportista;
import pe.com.proveperu.sgc.transporte.domain.model.Transportista;

public record TransportistaResponse(
    Long id,
    TipoDocumentoTransportista tipoDocumento,
    String numeroDocumento,
    String nombreRazonSocial,
    String empresaTransporte,
    String telefono,
    String direccion,
    String estado,
    Instant fechaRegistro,
    Instant fechaActualizacion
) {
    public static TransportistaResponse from(Transportista transportista) {
        return new TransportistaResponse(
            transportista.getId(),
            transportista.getTipoDocumento(),
            transportista.getNumeroDocumento(),
            transportista.getNombreRazonSocial(),
            transportista.getEmpresaTransporte(),
            transportista.getTelefono(),
            transportista.getDireccion(),
            transportista.getEstado().name(),
            transportista.getFechaRegistro(),
            transportista.getFechaActualizacion()
        );
    }
}
