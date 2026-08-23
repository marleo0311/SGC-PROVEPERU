package pe.com.proveperu.sgc.cliente.api.dto;

import pe.com.proveperu.sgc.cliente.application.dto.DatosDocumentoConsultado;
import pe.com.proveperu.sgc.cliente.domain.model.Cliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoDocumentoCliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoPersona;

public record ConsultaDocumentoResponse(
    boolean encontrado,
    String origen,
    boolean consultaExternaHabilitada,
    Long idCliente,
    String estadoCliente,
    TipoPersona tipoPersona,
    TipoDocumentoCliente tipoDocumento,
    String numeroDocumento,
    String nombres,
    String apellidos,
    String razonSocial,
    String nombreComercial,
    String nombreMostrar,
    String direccion,
    String estadoContribuyente,
    String condicionDomicilio,
    String mensaje
) {
    public static ConsultaDocumentoResponse local(Cliente cliente, boolean externaHabilitada) {
        ClienteResponse response = ClienteResponse.from(cliente);
        return new ConsultaDocumentoResponse(
            true,
            "LOCAL",
            externaHabilitada,
            response.id(),
            response.estado(),
            response.tipoPersona(),
            response.tipoDocumento(),
            response.numeroDocumento(),
            response.nombres(),
            response.apellidos(),
            response.razonSocial(),
            response.nombreComercial(),
            response.nombreMostrar(),
            response.direccion(),
            null,
            null,
            response.estado().equals("ACTIVO")
                ? "Cliente encontrado en el sistema"
                : "El cliente está registrado, pero se encuentra inactivo"
        );
    }

    public static ConsultaDocumentoResponse externa(DatosDocumentoConsultado datos) {
        return new ConsultaDocumentoResponse(
            true,
            "EXTERNO",
            true,
            null,
            null,
            datos.tipoPersona(),
            datos.tipoDocumento(),
            datos.numeroDocumento(),
            datos.nombres(),
            datos.apellidos(),
            datos.razonSocial(),
            datos.nombreComercial(),
            datos.nombreMostrar(),
            datos.direccion(),
            datos.estadoContribuyente(),
            datos.condicionDomicilio(),
            "Documento encontrado; confirma el registro para utilizarlo en la venta"
        );
    }

    public static ConsultaDocumentoResponse noEncontrada(
        TipoDocumentoCliente tipoDocumento,
        String numeroDocumento,
        boolean externaHabilitada,
        String mensaje
    ) {
        return new ConsultaDocumentoResponse(
            false,
            externaHabilitada ? "NO_ENCONTRADO" : "NO_CONFIGURADO",
            externaHabilitada,
            null,
            null,
            tipoDocumento == TipoDocumentoCliente.DNI ? TipoPersona.NATURAL : TipoPersona.JURIDICA,
            tipoDocumento,
            numeroDocumento,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            mensaje
        );
    }
}
