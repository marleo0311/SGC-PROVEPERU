package pe.com.proveperu.sgc.facturacionelectronica.application.service;

import java.util.List;
import org.springframework.stereotype.Service;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.ComunicacionBajaResponse;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.ArchivoElectronico;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.EstadoTicketSunat;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.ResultadoCdr;
import pe.com.proveperu.sgc.facturacionelectronica.application.port.SunatGateway;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.sunat.IntegracionSunatException;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.sunat.RechazoSunatException;

@Service
public class ComunicacionBajaService {
    private final ComunicacionBajaPersistenceService persistence;
    private final SunatGateway gateway;
    private final CdrParser cdrParser;

    public ComunicacionBajaService(ComunicacionBajaPersistenceService persistence, SunatGateway gateway, CdrParser cdrParser) {
        this.persistence = persistence; this.gateway = gateway; this.cdrParser = cdrParser;
    }

    public ComunicacionBajaResponse solicitar(Long idComprobante, String motivo, String login) { return persistence.solicitar(idComprobante, motivo, login); }
    public List<ComunicacionBajaResponse> listar() { return persistence.listar(); }
    public ComunicacionBajaResponse detalle(Long id) { return persistence.detalle(id); }

    public ComunicacionBajaResponse enviar(Long id) {
        var baja = persistence.marcarEnviando(id);
        if (baja.yaTerminada()) return baja.respuesta();
        try {
            String ticket = gateway.enviarResumen(baja.ruc(), baja.nombreZip(), baja.zip());
            return persistence.registrarTicket(baja.id(), ticket);
        } catch (RechazoSunatException exception) {
            persistence.registrarRechazo(id, exception.getCodigo(), exception.getMessage());
            throw exception;
        } catch (RuntimeException exception) {
            persistence.registrarError(id, exception.getMessage());
            throw integrar(exception, "No se pudo enviar la comunicación de baja");
        }
    }

    public ComunicacionBajaResponse consultar(Long id) {
        var consulta = persistence.marcarConsultando(id);
        if (consulta.yaTerminada()) return consulta.respuesta();
        try {
            EstadoTicketSunat estado = gateway.consultarTicket(consulta.ruc(), consulta.ticket());
            if (estado.procesando()) return persistence.registrarProcesando(consulta.id(), estado.codigo(), estado.mensaje());
            if (!estado.terminado() || estado.contenido() == null || estado.contenido().length == 0) return persistence.registrarSinCdr(consulta.id(), estado.codigo(), estado.mensaje());
            ResultadoCdr resultado = cdrParser.procesar(estado.contenido());
            return persistence.registrarCdr(consulta.id(), estado.codigo(), estado.contenido(), resultado);
        } catch (RuntimeException exception) {
            persistence.registrarError(id, exception.getMessage());
            throw integrar(exception, "No se pudo consultar el ticket de la comunicación de baja");
        }
    }

    public ArchivoElectronico xml(Long id) { return persistence.xml(id); }
    public ArchivoElectronico cdr(Long id) { return persistence.cdr(id); }
    private IntegracionSunatException integrar(RuntimeException e, String m) { return e instanceof IntegracionSunatException i ? i : new IntegracionSunatException(m, e); }
}
