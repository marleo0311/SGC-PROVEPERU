package pe.com.proveperu.sgc.cliente.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.cliente.api.dto.ConsultaDocumentoResponse;
import pe.com.proveperu.sgc.cliente.application.port.ConsultaDocumentoGateway;
import pe.com.proveperu.sgc.cliente.domain.model.TipoDocumentoCliente;
import pe.com.proveperu.sgc.cliente.infrastructure.documento.ConsultaDocumentoRateLimiter;
import pe.com.proveperu.sgc.cliente.infrastructure.persistence.ClienteRepository;
import pe.com.proveperu.sgc.shared.application.exception.SolicitudInvalidaException;

@Service
@RequiredArgsConstructor
public class ConsultaDocumentoService {

    private final ClienteRepository clienteRepository;
    private final ConsultaDocumentoGateway consultaDocumentoGateway;
    private final ConsultaDocumentoRateLimiter rateLimiter;

    @Transactional(readOnly = true)
    public ConsultaDocumentoResponse consultar(
        TipoDocumentoCliente tipoDocumento,
        String numeroDocumento,
        String solicitante
    ) {
        String documento = numeroDocumento == null ? "" : numeroDocumento.strip();
        validar(tipoDocumento, documento);
        boolean consultaExternaDisponible = consultaDocumentoGateway.disponible(tipoDocumento);
        return clienteRepository.findByNumeroDocumento(documento)
            .map(cliente -> ConsultaDocumentoResponse.local(cliente, consultaExternaDisponible))
            .orElseGet(() -> consultarExternamente(
                tipoDocumento,
                documento,
                consultaExternaDisponible,
                solicitante
            ));
    }

    private ConsultaDocumentoResponse consultarExternamente(
        TipoDocumentoCliente tipoDocumento,
        String documento,
        boolean disponible,
        String solicitante
    ) {
        if (!disponible) {
            String mensaje = tipoDocumento == TipoDocumentoCliente.DNI
                ? "El DNI no está registrado localmente. La consulta externa de DNI no está habilitada; regístralo manualmente o configura un proveedor autorizado."
                : "El RUC no está registrado localmente. Configura DOCUMENT_LOOKUP_ENABLED y DOCUMENT_LOOKUP_TOKEN para completar sus datos automáticamente.";
            return ConsultaDocumentoResponse.noEncontrada(
                tipoDocumento,
                documento,
                false,
                mensaje
            );
        }
        rateLimiter.verificar(solicitante);
        return consultaDocumentoGateway.consultar(tipoDocumento, documento)
            .map(ConsultaDocumentoResponse::externa)
            .orElseGet(() -> ConsultaDocumentoResponse.noEncontrada(
                tipoDocumento,
                documento,
                true,
                "No se encontraron datos para el documento ingresado"
            ));
    }

    private void validar(TipoDocumentoCliente tipoDocumento, String documento) {
        if (tipoDocumento == null) {
            throw new SolicitudInvalidaException("Selecciona el tipo de documento");
        }
        String patron = tipoDocumento == TipoDocumentoCliente.DNI ? "^[0-9]{8}$" : "^[0-9]{11}$";
        if (!documento.matches(patron)) {
            throw new SolicitudInvalidaException(
                tipoDocumento == TipoDocumentoCliente.DNI
                    ? "El DNI debe tener exactamente 8 dígitos"
                    : "El RUC debe tener exactamente 11 dígitos"
            );
        }
        if (tipoDocumento == TipoDocumentoCliente.RUC && !ValidadorRuc.esValido(documento)) {
            throw new SolicitudInvalidaException("El RUC no tiene un dígito verificador válido");
        }
    }
}
