package pe.com.proveperu.sgc.proveedor.api.dto;

import java.time.Instant;
import pe.com.proveperu.sgc.proveedor.domain.model.Proveedor;

public record ProveedorResponse(
    Long id,
    String ruc,
    String razonSocial,
    String nombreComercial,
    String direccion,
    String telefono,
    String correo,
    String personaContacto,
    String estado,
    Instant fechaRegistro,
    Instant fechaActualizacion
) {
    public static ProveedorResponse from(Proveedor proveedor) {
        return new ProveedorResponse(
            proveedor.getId(),
            proveedor.getRuc(),
            proveedor.getRazonSocial(),
            proveedor.getNombreComercial(),
            proveedor.getDireccion(),
            proveedor.getTelefono(),
            proveedor.getCorreo(),
            proveedor.getPersonaContacto(),
            proveedor.getEstado().name(),
            proveedor.getFechaRegistro(),
            proveedor.getFechaActualizacion()
        );
    }
}
