package pe.com.proveperu.sgc.facturacionelectronica.application.service;

import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.comprobante.domain.model.Comprobante;
import pe.com.proveperu.sgc.comprobante.domain.model.EstadoComprobante;
import pe.com.proveperu.sgc.comprobante.infrastructure.persistence.ComprobanteRepository;
import pe.com.proveperu.sgc.configuracion.domain.model.Empresa;
import pe.com.proveperu.sgc.configuracion.infrastructure.persistence.EmpresaRepository;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.EnvioSunatResponse;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.ArchivoElectronico;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.DocumentoFirmado;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.ResultadoCdr;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.AmbienteSunat;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.EnvioSunat;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.EstadoEnvioSunat;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.config.SunatProperties;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.persistence.EnvioSunatRepository;
import pe.com.proveperu.sgc.security.application.exception.OperacionNoPermitidaException;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;
import pe.com.proveperu.sgc.venta.domain.model.TipoComprobanteVenta;

@Service
public class EnvioSunatPersistenceService {

    private final ComprobanteRepository comprobanteRepository;
    private final EmpresaRepository empresaRepository;
    private final EnvioSunatRepository envioRepository;
    private final DocumentoElectronicoService documentoService;
    private final SunatProperties properties;

    public EnvioSunatPersistenceService(
        ComprobanteRepository comprobanteRepository,
        EmpresaRepository empresaRepository,
        EnvioSunatRepository envioRepository,
        DocumentoElectronicoService documentoService,
        SunatProperties properties
    ) {
        this.comprobanteRepository = comprobanteRepository;
        this.empresaRepository = empresaRepository;
        this.envioRepository = envioRepository;
        this.documentoService = documentoService;
        this.properties = properties;
    }

    @Transactional
    public EnvioSunatResponse preparar(Long idComprobante) {
        Comprobante comprobante = comprobanteRepository.findDetalleById(idComprobante)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe el comprobante solicitado"
            ));
        validarEnvioIndividual(comprobante);
        EnvioSunat existente = envioRepository.findForUpdateByComprobanteId(idComprobante)
            .orElse(null);
        if (existente != null && existente.getEstado().aceptado()) {
            return EnvioSunatResponse.from(existente);
        }
        if (existente != null && existente.getEstado() == EstadoEnvioSunat.ENVIANDO) {
            throw new OperacionNoPermitidaException("El comprobante ya se encuentra en proceso de envío");
        }
        Empresa empresa = empresa(comprobante);
        DocumentoFirmado documento = documentoService.preparar(comprobante, empresa);
        EnvioSunat envio = existente == null ? new EnvioSunat() : existente;
        envio.setComprobante(comprobante);
        envio.setAmbiente(properties.getAmbiente());
        envio.setEstado(EstadoEnvioSunat.GENERADO);
        envio.setNombreArchivo(documento.nombreBase());
        envio.setHashXml(documento.hashSha256());
        envio.setXmlFirmado(documento.xmlFirmado());
        envio.setZipEnviado(documento.zip());
        envio.setCdrZip(null);
        envio.setTicket(null);
        envio.setCodigoRespuesta(null);
        envio.setDescripcionRespuesta(null);
        envio.setObservaciones(null);
        envio.setErrorUltimo(null);
        envio.setFechaGeneracion(Instant.now());
        envio.setFechaRespuesta(null);
        envio = envioRepository.saveAndFlush(envio);
        comprobante.setEnvioSunat(envio);
        comprobante.setEstado(EstadoComprobante.PENDIENTE_ENVIO);
        return EnvioSunatResponse.from(envio);
    }

    @Transactional
    public EnvioPreparado marcarEnviando(Long idComprobante) {
        EnvioSunat envio = envioRepository.findForUpdateByComprobanteId(idComprobante)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "Primero genera el XML electrónico del comprobante"
            ));
        if (envio.getEstado().aceptado()) {
            return EnvioPreparado.aceptado(EnvioSunatResponse.from(envio));
        }
        if (envio.getEstado() == EstadoEnvioSunat.ENVIANDO) {
            throw new OperacionNoPermitidaException("El comprobante ya se encuentra en proceso de envío");
        }
        if (
            properties.getAmbiente() == AmbienteSunat.PRODUCCION
                && envio.getComprobante().getTipo() == TipoComprobanteVenta.BOLETA
        ) {
            throw new OperacionNoPermitidaException(
                "Las boletas de producción se envían desde Resúmenes SUNAT, no de forma individual"
            );
        }
        envio.setEstado(EstadoEnvioSunat.ENVIANDO);
        envio.setIntentos(envio.getIntentos() + 1);
        envio.setFechaUltimoIntento(Instant.now());
        envio.setErrorUltimo(null);
        envioRepository.saveAndFlush(envio);
        Empresa empresa = empresa(envio.getComprobante());
        return new EnvioPreparado(
            envio.getId(),
            empresa.getRuc(),
            envio.getNombreArchivo() + ".zip",
            envio.getZipEnviado(),
            null
        );
    }

    @Transactional
    public EnvioSunatResponse registrarCdr(
        Long idEnvio,
        byte[] cdrZip,
        ResultadoCdr resultado
    ) {
        EnvioSunat envio = envioRepository.findById(idEnvio)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe el intento de envío SUNAT"
            ));
        envio.setCdrZip(cdrZip);
        envio.setCodigoRespuesta(resultado.codigo());
        envio.setDescripcionRespuesta(resultado.descripcion());
        envio.setObservaciones(String.join(System.lineSeparator(), resultado.observaciones()));
        envio.setFechaRespuesta(Instant.now());
        envio.setEstado(resultado.aceptado()
            ? resultado.observaciones().isEmpty()
                ? EstadoEnvioSunat.ACEPTADO
                : EstadoEnvioSunat.ACEPTADO_CON_OBSERVACIONES
            : EstadoEnvioSunat.RECHAZADO);
        if (resultado.aceptado()) {
            envio.getComprobante().setEstado(EstadoComprobante.EMITIDO);
        } else {
            envio.getComprobante().setEstado(EstadoComprobante.PENDIENTE_ENVIO);
        }
        return EnvioSunatResponse.from(envioRepository.saveAndFlush(envio));
    }

    @Transactional
    public void registrarError(Long idEnvio, String message) {
        envioRepository.findById(idEnvio).ifPresent(envio -> {
            envio.setEstado(EstadoEnvioSunat.ERROR_COMUNICACION);
            envio.setErrorUltimo(recortar(message, 2000));
            envioRepository.saveAndFlush(envio);
        });
    }

    @Transactional
    public void registrarRechazoSoap(Long idEnvio, String codigo, String message) {
        envioRepository.findById(idEnvio).ifPresent(envio -> {
            envio.setEstado(EstadoEnvioSunat.RECHAZADO);
            envio.setCodigoRespuesta(recortar(codigo, 20));
            envio.setDescripcionRespuesta(recortar(message, 1000));
            envio.setErrorUltimo(null);
            envio.setFechaRespuesta(Instant.now());
            envio.getComprobante().setEstado(EstadoComprobante.PENDIENTE_ENVIO);
            envioRepository.saveAndFlush(envio);
        });
    }

    @Transactional(readOnly = true)
    public Optional<EnvioSunatResponse> consultar(Long idComprobante) {
        validarExisteComprobante(idComprobante);
        return envioRepository.findByComprobanteId(idComprobante).map(EnvioSunatResponse::from);
    }

    @Transactional(readOnly = true)
    public ArchivoElectronico xml(Long idComprobante) {
        EnvioSunat envio = envio(idComprobante);
        return new ArchivoElectronico(
            envio.getNombreArchivo() + ".xml",
            "application/xml",
            envio.getXmlFirmado()
        );
    }

    @Transactional(readOnly = true)
    public ArchivoElectronico cdr(Long idComprobante) {
        EnvioSunat envio = envio(idComprobante);
        if (envio.getCdrZip() == null || envio.getCdrZip().length == 0) {
            throw new RecursoNoEncontradoException("El comprobante todavía no tiene un CDR");
        }
        return new ArchivoElectronico(
            "R-" + envio.getNombreArchivo() + ".zip",
            "application/zip",
            envio.getCdrZip()
        );
    }

    private Empresa empresa(Comprobante comprobante) {
        return empresaRepository.findById(comprobante.getVenta().getSede().getIdEmpresa())
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la empresa emisora del comprobante"
            ));
    }

    private void validarEnvioIndividual(Comprobante comprobante) {
        if (properties.getAmbiente() == AmbienteSunat.PRODUCCION
            && comprobante.getTipo() == TipoComprobanteVenta.BOLETA) {
            throw new OperacionNoPermitidaException(
                "Las boletas de producción se preparan y envían desde Resúmenes SUNAT"
            );
        }
    }

    private EnvioSunat envio(Long idComprobante) {
        return envioRepository.findByComprobanteId(idComprobante)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "El comprobante todavía no tiene un documento electrónico generado"
            ));
    }

    private void validarExisteComprobante(Long idComprobante) {
        if (!comprobanteRepository.existsById(idComprobante)) {
            throw new RecursoNoEncontradoException("No existe el comprobante solicitado");
        }
    }

    private String recortar(String value, int max) {
        if (value == null || value.isBlank()) {
            return "Error de comunicación sin detalle";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    public record EnvioPreparado(
        Long idEnvio,
        String ruc,
        String nombreZip,
        byte[] zip,
        EnvioSunatResponse respuestaAceptada
    ) {
        static EnvioPreparado aceptado(EnvioSunatResponse response) {
            return new EnvioPreparado(null, null, null, null, response);
        }

        boolean yaAceptado() {
            return respuestaAceptada != null;
        }
    }
}
