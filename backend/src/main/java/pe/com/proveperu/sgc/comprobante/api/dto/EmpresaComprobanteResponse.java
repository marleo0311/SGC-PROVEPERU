package pe.com.proveperu.sgc.comprobante.api.dto;

import pe.com.proveperu.sgc.configuracion.domain.model.Empresa;
import pe.com.proveperu.sgc.inventario.domain.model.Sede;

public record EmpresaComprobanteResponse(
    String ruc,
    String razonSocial,
    String nombreComercial,
    String direccion,
    String telefono,
    Long idSede,
    String sede,
    String direccionSede
) {
    public static EmpresaComprobanteResponse from(Empresa empresa, Sede sede) {
        return new EmpresaComprobanteResponse(
            empresa.getRuc(),
            empresa.getRazonSocial(),
            empresa.getNombreComercial(),
            empresa.getDireccion(),
            empresa.getTelefono(),
            sede.getId(),
            sede.getNombre(),
            sede.getDireccion()
        );
    }
}
