package pe.com.proveperu.sgc.facturacionelectronica.application.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.ConfiguracionSunatResponse;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.EnvioSunatResponse;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.ArchivoElectronico;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.ResultadoCdr;
import pe.com.proveperu.sgc.facturacionelectronica.application.port.SunatGateway;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.config.SunatProperties;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.sunat.IntegracionSunatException;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.sunat.RechazoSunatException;

@Service
public class FacturacionElectronicaService {

    private final EnvioSunatPersistenceService persistenceService;
    private final SunatGateway sunatGateway;
    private final CdrParser cdrParser;
    private final SunatProperties properties;

    public FacturacionElectronicaService(
        EnvioSunatPersistenceService persistenceService,
        SunatGateway sunatGateway,
        CdrParser cdrParser,
        SunatProperties properties
    ) {
        this.persistenceService = persistenceService;
        this.sunatGateway = sunatGateway;
        this.cdrParser = cdrParser;
        this.properties = properties;
    }

    public EnvioSunatResponse preparar(Long idComprobante) {
        return persistenceService.preparar(idComprobante);
    }

    public EnvioSunatResponse enviar(Long idComprobante) {
        Optional<EnvioSunatResponse> existente = persistenceService.consultar(idComprobante);
        if (existente.isEmpty()) {
            persistenceService.preparar(idComprobante);
        }
        EnvioSunatPersistenceService.EnvioPreparado envio = persistenceService
            .marcarEnviando(idComprobante);
        if (envio.yaAceptado()) {
            return envio.respuestaAceptada();
        }
        try {
            byte[] cdrZip = sunatGateway.enviarComprobante(
                envio.ruc(),
                envio.nombreZip(),
                envio.zip()
            );
            ResultadoCdr resultado = cdrParser.procesar(cdrZip);
            return persistenceService.registrarCdr(envio.idEnvio(), cdrZip, resultado);
        } catch (RechazoSunatException exception) {
            persistenceService.registrarRechazoSoap(
                envio.idEnvio(),
                exception.getCodigo(),
                exception.getMessage()
            );
            throw exception;
        } catch (RuntimeException exception) {
            persistenceService.registrarError(envio.idEnvio(), exception.getMessage());
            if (exception instanceof IntegracionSunatException integracion) {
                throw integracion;
            }
            throw new IntegracionSunatException(
                "No se pudo completar el envío electrónico",
                exception
            );
        }
    }

    public Optional<EnvioSunatResponse> consultar(Long idComprobante) {
        return persistenceService.consultar(idComprobante);
    }

    public ArchivoElectronico xml(Long idComprobante) {
        return persistenceService.xml(idComprobante);
    }

    public ArchivoElectronico cdr(Long idComprobante) {
        return persistenceService.cdr(idComprobante);
    }

    public ConfiguracionSunatResponse configuracion() {
        return ConfiguracionSunatResponse.from(properties);
    }
}
