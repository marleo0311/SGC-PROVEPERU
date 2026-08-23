package pe.com.proveperu.sgc.cliente.application.port;

import java.util.Optional;
import pe.com.proveperu.sgc.cliente.application.dto.DatosDocumentoConsultado;
import pe.com.proveperu.sgc.cliente.domain.model.TipoDocumentoCliente;

public interface ConsultaDocumentoGateway {

    boolean disponible(TipoDocumentoCliente tipoDocumento);

    Optional<DatosDocumentoConsultado> consultar(
        TipoDocumentoCliente tipoDocumento,
        String numeroDocumento
    );
}
