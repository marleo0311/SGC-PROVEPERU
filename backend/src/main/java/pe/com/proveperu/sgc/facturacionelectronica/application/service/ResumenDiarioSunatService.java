package pe.com.proveperu.sgc.facturacionelectronica.application.service;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.ResumenDiarioSunatResponse;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.ArchivoElectronico;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.EstadoTicketSunat;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.ResultadoCdr;
import pe.com.proveperu.sgc.facturacionelectronica.application.port.SunatGateway;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.sunat.IntegracionSunatException;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.sunat.RechazoSunatException;

@Service
public class ResumenDiarioSunatService {

    private final ResumenDiarioSunatPersistenceService persistenceService;
    private final SunatGateway sunatGateway;
    private final CdrParser cdrParser;

    public ResumenDiarioSunatService(
        ResumenDiarioSunatPersistenceService persistenceService,
        SunatGateway sunatGateway,
        CdrParser cdrParser
    ) {
        this.persistenceService = persistenceService;
        this.sunatGateway = sunatGateway;
        this.cdrParser = cdrParser;
    }

    public List<ResumenDiarioSunatResponse> preparar(LocalDate fechaDocumentos) {
        return persistenceService.preparar(fechaDocumentos);
    }

    public List<ResumenDiarioSunatResponse> listar(LocalDate fechaDocumentos) {
        return persistenceService.listar(fechaDocumentos);
    }

    public ResumenDiarioSunatResponse detalle(Long id) {
        return persistenceService.detalle(id);
    }

    public ResumenDiarioSunatResponse enviar(Long id) {
        ResumenDiarioSunatPersistenceService.ResumenPreparado resumen =
            persistenceService.marcarEnviando(id);
        if (resumen.yaAceptado()) {
            return resumen.respuestaAceptada();
        }
        try {
            String ticket = sunatGateway.enviarResumen(
                resumen.ruc(),
                resumen.nombreZip(),
                resumen.zip()
            );
            return persistenceService.registrarTicket(resumen.id(), ticket);
        } catch (RechazoSunatException exception) {
            persistenceService.registrarRechazoSoap(
                resumen.id(),
                exception.getCodigo(),
                exception.getMessage()
            );
            throw exception;
        } catch (RuntimeException exception) {
            persistenceService.registrarError(resumen.id(), exception.getMessage());
            throw integrar(exception, "No se pudo enviar el resumen diario");
        }
    }

    public ResumenDiarioSunatResponse consultarEstado(Long id) {
        ResumenDiarioSunatPersistenceService.ConsultaTicket consulta =
            persistenceService.marcarConsultando(id);
        if (consulta.yaTerminada()) {
            return consulta.respuestaTerminada();
        }
        try {
            EstadoTicketSunat estado = sunatGateway.consultarTicket(
                consulta.ruc(),
                consulta.ticket()
            );
            if (estado.procesando()) {
                return persistenceService.registrarProcesando(
                    consulta.id(),
                    estado.codigo(),
                    estado.mensaje()
                );
            }
            if (!estado.terminado()) {
                throw new IntegracionSunatException(
                    "SUNAT devolvió un estado de ticket no reconocido: " + estado.codigo()
                );
            }
            if (estado.contenido() == null || estado.contenido().length == 0) {
                return persistenceService.registrarTerminadoSinCdr(
                    consulta.id(),
                    estado.codigo(),
                    estado.mensaje()
                );
            }
            ResultadoCdr resultado = cdrParser.procesar(estado.contenido());
            return persistenceService.registrarCdr(
                consulta.id(),
                estado.codigo(),
                estado.contenido(),
                resultado
            );
        } catch (RuntimeException exception) {
            persistenceService.registrarError(consulta.id(), exception.getMessage());
            throw integrar(exception, "No se pudo consultar el ticket del resumen diario");
        }
    }

    public ArchivoElectronico xml(Long id) {
        return persistenceService.xml(id);
    }

    public ArchivoElectronico cdr(Long id) {
        return persistenceService.cdr(id);
    }

    private IntegracionSunatException integrar(RuntimeException exception, String mensaje) {
        return exception instanceof IntegracionSunatException integracion
            ? integracion
            : new IntegracionSunatException(mensaje, exception);
    }
}
