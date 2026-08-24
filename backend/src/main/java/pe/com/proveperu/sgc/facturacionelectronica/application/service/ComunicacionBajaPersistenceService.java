package pe.com.proveperu.sgc.facturacionelectronica.application.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.comprobante.domain.model.Comprobante;
import pe.com.proveperu.sgc.comprobante.domain.model.EstadoComprobante;
import pe.com.proveperu.sgc.comprobante.infrastructure.persistence.ComprobanteRepository;
import pe.com.proveperu.sgc.configuracion.domain.model.Empresa;
import pe.com.proveperu.sgc.configuracion.infrastructure.persistence.EmpresaRepository;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.ComunicacionBajaResponse;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.ArchivoElectronico;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.DocumentoFirmado;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.ResultadoCdr;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.AmbienteSunat;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.ComunicacionBajaSunat;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.EstadoResumenDiarioSunat;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.config.SunatProperties;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.persistence.ComunicacionBajaRepository;
import pe.com.proveperu.sgc.security.application.exception.OperacionNoPermitidaException;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;
import pe.com.proveperu.sgc.security.infrastructure.persistence.UsuarioRepository;
import pe.com.proveperu.sgc.shared.application.exception.ReglaNegocioException;
import pe.com.proveperu.sgc.venta.domain.model.TipoComprobanteVenta;

@Service
public class ComunicacionBajaPersistenceService {
    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    private final ComunicacionBajaRepository repository;
    private final ComprobanteRepository comprobanteRepository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final GeneradorComunicacionBajaUblService generador;
    private final DocumentoElectronicoService documentoService;
    private final SunatProperties properties;

    public ComunicacionBajaPersistenceService(
        ComunicacionBajaRepository repository,
        ComprobanteRepository comprobanteRepository,
        EmpresaRepository empresaRepository,
        UsuarioRepository usuarioRepository,
        GeneradorComunicacionBajaUblService generador,
        DocumentoElectronicoService documentoService,
        SunatProperties properties
    ) {
        this.repository = repository;
        this.comprobanteRepository = comprobanteRepository;
        this.empresaRepository = empresaRepository;
        this.usuarioRepository = usuarioRepository;
        this.generador = generador;
        this.documentoService = documentoService;
        this.properties = properties;
    }

    @Transactional
    public ComunicacionBajaResponse solicitar(Long idComprobante, String motivo, String login) {
        Comprobante comprobante = comprobanteRepository.findDetalleById(idComprobante)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el comprobante"));
        validar(comprobante);
        var usuario = usuarioRepository.findByUsuarioLoginIgnoreCase(login)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario autenticado"));
        LocalDate fechaDocumento = comprobante.getFechaEmision().atZone(LIMA).toLocalDate();
        String motivoLimpio = motivo.strip();
        comprobante.setEstado(EstadoComprobante.BAJA_PENDIENTE);
        comprobante.setMotivoAnulacion(motivoLimpio);
        comprobante.setUsuarioAnulacion(usuario);

        if (comprobante.getTipo() == TipoComprobanteVenta.BOLETA) {
            return ComunicacionBajaResponse.boleta(
                comprobante.getId(), comprobante.getNumeroCompleto(), motivoLimpio,
                properties.getAmbiente(), fechaDocumento
            );
        }
        repository.findByComprobanteIdAndAmbiente(comprobante.getId(), properties.getAmbiente())
            .ifPresent(existing -> { throw new OperacionNoPermitidaException("El comprobante ya tiene una comunicación de baja en este ambiente"); });
        Empresa empresa = empresa(comprobante);
        ComunicacionBajaSunat baja = new ComunicacionBajaSunat();
        baja.setComprobante(comprobante);
        baja.setUsuario(usuario);
        baja.setAmbiente(properties.getAmbiente());
        baja.setFechaDocumento(fechaDocumento);
        baja.setFechaGeneracion(LocalDate.now(LIMA));
        baja.setCorrelativo(Math.toIntExact(repository.siguienteCorrelativo()));
        baja.setMotivo(motivoLimpio);
        baja.setEstado(EstadoResumenDiarioSunat.GENERADO);
        baja.setFechaCreacion(Instant.now());
        DocumentoFirmado documento = documentoService.preparar(generador.generar(baja, empresa));
        baja.setNombreArchivo(documento.nombreBase());
        baja.setHashXml(documento.hashSha256());
        baja.setXmlFirmado(documento.xmlFirmado());
        baja.setZipEnviado(documento.zip());
        return ComunicacionBajaResponse.from(repository.saveAndFlush(baja));
    }

    @Transactional(readOnly = true)
    public List<ComunicacionBajaResponse> listar() {
        return repository.findAllByOrderByFechaCreacionDesc().stream()
            .map(ComunicacionBajaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ComunicacionBajaResponse detalle(Long id) {
        return ComunicacionBajaResponse.from(baja(id));
    }

    @Transactional
    public BajaPreparada marcarEnviando(Long id) {
        ComunicacionBajaSunat baja = bloqueada(id);
        validarAmbiente(baja.getAmbiente());
        if (baja.getEstado().aceptado()) return BajaPreparada.terminada(ComunicacionBajaResponse.from(baja));
        if (baja.getTicket() != null && !baja.getTicket().isBlank()) throw new OperacionNoPermitidaException("La baja ya tiene ticket; consulta su estado");
        baja.setEstado(EstadoResumenDiarioSunat.ENVIANDO);
        baja.setIntentosEnvio(baja.getIntentosEnvio() + 1);
        baja.setFechaUltimoIntento(Instant.now());
        baja.setErrorUltimo(null);
        repository.saveAndFlush(baja);
        return new BajaPreparada(baja.getId(), empresa(baja.getComprobante()).getRuc(), baja.getNombreArchivo() + ".zip", baja.getZipEnviado(), null);
    }

    @Transactional
    public ComunicacionBajaResponse registrarTicket(Long id, String ticket) {
        ComunicacionBajaSunat baja = bloqueada(id);
        baja.setTicket(ticket); baja.setEstado(EstadoResumenDiarioSunat.TICKET_RECIBIDO); baja.setErrorUltimo(null);
        return ComunicacionBajaResponse.from(repository.saveAndFlush(baja));
    }

    @Transactional
    public ConsultaBaja marcarConsultando(Long id) {
        ComunicacionBajaSunat baja = bloqueada(id);
        if (baja.getEstado().aceptado() || baja.getEstado() == EstadoResumenDiarioSunat.RECHAZADO) return ConsultaBaja.terminada(ComunicacionBajaResponse.from(baja));
        if (baja.getTicket() == null || baja.getTicket().isBlank()) throw new OperacionNoPermitidaException("Primero envía la comunicación para obtener un ticket");
        baja.setConsultasEstado(baja.getConsultasEstado() + 1); baja.setFechaUltimaConsulta(Instant.now()); baja.setErrorUltimo(null);
        repository.saveAndFlush(baja);
        return new ConsultaBaja(baja.getId(), empresa(baja.getComprobante()).getRuc(), baja.getTicket(), null);
    }

    @Transactional
    public ComunicacionBajaResponse registrarProcesando(Long id, String codigo, String mensaje) {
        ComunicacionBajaSunat baja = bloqueada(id); baja.setEstado(EstadoResumenDiarioSunat.PROCESANDO); baja.setCodigoEstadoTicket(codigo); baja.setDescripcionRespuesta(recortar(mensaje, 1000)); baja.setErrorUltimo(null);
        return ComunicacionBajaResponse.from(repository.saveAndFlush(baja));
    }

    @Transactional
    public ComunicacionBajaResponse registrarCdr(Long id, String codigoTicket, byte[] cdr, ResultadoCdr resultado) {
        ComunicacionBajaSunat baja = bloqueada(id); baja.setCdrZip(cdr); baja.setCodigoEstadoTicket(codigoTicket); baja.setCodigoRespuesta(resultado.codigo()); baja.setDescripcionRespuesta(resultado.descripcion()); baja.setObservaciones(String.join(System.lineSeparator(), resultado.observaciones())); baja.setFechaRespuesta(Instant.now()); baja.setErrorUltimo(null);
        baja.setEstado(resultado.aceptado() ? resultado.observaciones().isEmpty() ? EstadoResumenDiarioSunat.ACEPTADO : EstadoResumenDiarioSunat.ACEPTADO_CON_OBSERVACIONES : EstadoResumenDiarioSunat.RECHAZADO);
        baja.getComprobante().setEstado(resultado.aceptado() ? EstadoComprobante.ANULADO : EstadoComprobante.EMITIDO);
        if (resultado.aceptado()) baja.getComprobante().setFechaAnulacion(Instant.now());
        return ComunicacionBajaResponse.from(repository.saveAndFlush(baja));
    }

    @Transactional
    public ComunicacionBajaResponse registrarSinCdr(Long id, String codigo, String mensaje) {
        ComunicacionBajaSunat baja = bloqueada(id); baja.setEstado(EstadoResumenDiarioSunat.RECHAZADO); baja.setCodigoRespuesta(codigo); baja.setDescripcionRespuesta(recortar(mensaje, 1000)); baja.setFechaRespuesta(Instant.now()); baja.getComprobante().setEstado(EstadoComprobante.EMITIDO);
        return ComunicacionBajaResponse.from(repository.saveAndFlush(baja));
    }

    @Transactional
    public void registrarError(Long id, String mensaje) { repository.findForUpdateById(id).ifPresent(b -> { b.setEstado(EstadoResumenDiarioSunat.ERROR_COMUNICACION); b.setErrorUltimo(recortar(mensaje, 2000)); repository.saveAndFlush(b); }); }

    @Transactional
    public void registrarRechazo(Long id, String codigo, String mensaje) { repository.findForUpdateById(id).ifPresent(b -> { b.setEstado(EstadoResumenDiarioSunat.RECHAZADO); b.setCodigoRespuesta(codigo); b.setDescripcionRespuesta(recortar(mensaje, 1000)); b.setFechaRespuesta(Instant.now()); b.getComprobante().setEstado(EstadoComprobante.EMITIDO); repository.saveAndFlush(b); }); }

    @Transactional(readOnly = true)
    public ArchivoElectronico xml(Long id) { ComunicacionBajaSunat b = baja(id); return new ArchivoElectronico(b.getNombreArchivo() + ".xml", "application/xml", b.getXmlFirmado()); }
    @Transactional(readOnly = true)
    public ArchivoElectronico cdr(Long id) { ComunicacionBajaSunat b = baja(id); if (b.getCdrZip() == null) throw new RecursoNoEncontradoException("La comunicación todavía no tiene CDR"); return new ArchivoElectronico("R-" + b.getNombreArchivo() + ".zip", "application/zip", b.getCdrZip()); }

    private void validar(Comprobante comprobante) {
        validarAmbiente(comprobante.getAmbiente());
        if (comprobante.getTipo() == TipoComprobanteVenta.NOTA_VENTA) throw new OperacionNoPermitidaException("Una nota de venta interna no se comunica a SUNAT");
        if (comprobante.getEstado() != EstadoComprobante.EMITIDO) throw new OperacionNoPermitidaException("El comprobante no está disponible para solicitar la baja");
        LocalDate fecha = comprobante.getFechaEmision().atZone(LIMA).toLocalDate();
        if (LocalDate.now(LIMA).isAfter(fecha.plusDays(7))) throw new ReglaNegocioException("La baja excede el plazo de siete días calendario; utiliza una nota de crédito cuando corresponda");
    }
    private void validarAmbiente(AmbienteSunat ambiente) {
        if (ambiente != properties.getAmbiente()) {
            throw new OperacionNoPermitidaException(
                "El documento fue generado en " + ambiente
                    + " y no puede procesarse en " + properties.getAmbiente()
            );
        }
    }
    private Empresa empresa(Comprobante c) { return empresaRepository.findById(c.getVenta().getSede().getIdEmpresa()).orElseThrow(() -> new RecursoNoEncontradoException("No existe la empresa emisora")); }
    private ComunicacionBajaSunat baja(Long id) { return repository.findDetalleById(id).orElseThrow(() -> new RecursoNoEncontradoException("No existe la comunicación de baja")); }
    private ComunicacionBajaSunat bloqueada(Long id) { return repository.findForUpdateById(id).orElseThrow(() -> new RecursoNoEncontradoException("No existe la comunicación de baja")); }
    private String recortar(String v, int max) { if (v == null) return null; return v.length() <= max ? v : v.substring(0, max); }

    public record BajaPreparada(Long id, String ruc, String nombreZip, byte[] zip, ComunicacionBajaResponse respuesta) { static BajaPreparada terminada(ComunicacionBajaResponse r) { return new BajaPreparada(null, null, null, null, r); } boolean yaTerminada() { return respuesta != null; } }
    public record ConsultaBaja(Long id, String ruc, String ticket, ComunicacionBajaResponse respuesta) { static ConsultaBaja terminada(ComunicacionBajaResponse r) { return new ConsultaBaja(null, null, null, r); } boolean yaTerminada() { return respuesta != null; } }
}
