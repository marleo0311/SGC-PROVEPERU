package pe.com.proveperu.sgc.facturacionelectronica.application.service;

import java.util.List;
import org.springframework.stereotype.Service;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.NotaElectronicaCrearRequest;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.NotaElectronicaResponse;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.ArchivoElectronico;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.ResultadoCdr;
import pe.com.proveperu.sgc.facturacionelectronica.application.port.SunatGateway;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.sunat.IntegracionSunatException;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.sunat.RechazoSunatException;

@Service
public class NotaElectronicaService {

    private final NotaElectronicaPersistenceService persistenceService;
    private final SunatGateway sunatGateway;
    private final CdrParser cdrParser;

    public NotaElectronicaService(
        NotaElectronicaPersistenceService persistenceService,
        SunatGateway sunatGateway,
        CdrParser cdrParser
    ) {
        this.persistenceService = persistenceService;
        this.sunatGateway = sunatGateway;
        this.cdrParser = cdrParser;
    }

    public NotaElectronicaResponse crear(
        Long idComprobante,
        NotaElectronicaCrearRequest request,
        String login
    ) {
        return persistenceService.crear(idComprobante, request, login);
    }

    public List<NotaElectronicaResponse> listar(Long idComprobante) {
        return persistenceService.listar(idComprobante);
    }

    public NotaElectronicaResponse detalle(Long id) {
        return persistenceService.detalle(id);
    }

    public NotaElectronicaResponse enviar(Long id) {
        NotaElectronicaPersistenceService.NotaPreparada nota =
            persistenceService.marcarEnviando(id);
        if (nota.yaAceptada()) {
            return nota.aceptada();
        }
        try {
            byte[] cdrZip = sunatGateway.enviarComprobante(
                nota.ruc(),
                nota.nombreZip(),
                nota.zip()
            );
            ResultadoCdr resultado = cdrParser.procesar(cdrZip);
            return persistenceService.registrarCdr(nota.id(), cdrZip, resultado);
        } catch (RechazoSunatException exception) {
            persistenceService.registrarRechazo(id, exception.getCodigo(), exception.getMessage());
            throw exception;
        } catch (RuntimeException exception) {
            persistenceService.registrarError(id, exception.getMessage());
            if (exception instanceof IntegracionSunatException integracion) {
                throw integracion;
            }
            throw new IntegracionSunatException(
                "No se pudo completar el envío de la nota electrónica",
                exception
            );
        }
    }

    public ArchivoElectronico xml(Long id) {
        return persistenceService.xml(id);
    }

    public ArchivoElectronico cdr(Long id) {
        return persistenceService.cdr(id);
    }
}
