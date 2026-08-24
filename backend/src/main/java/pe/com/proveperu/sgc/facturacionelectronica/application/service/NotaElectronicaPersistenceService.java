package pe.com.proveperu.sgc.facturacionelectronica.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.comprobante.application.service.CorrelativoComprobanteService;
import pe.com.proveperu.sgc.comprobante.domain.model.Comprobante;
import pe.com.proveperu.sgc.comprobante.domain.model.EstadoComprobante;
import pe.com.proveperu.sgc.comprobante.domain.model.TipoNumeracionComprobante;
import pe.com.proveperu.sgc.comprobante.infrastructure.persistence.ComprobanteRepository;
import pe.com.proveperu.sgc.configuracion.domain.model.Empresa;
import pe.com.proveperu.sgc.configuracion.infrastructure.persistence.EmpresaRepository;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.NotaElectronicaCrearRequest;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.NotaElectronicaResponse;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.ArchivoElectronico;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.DocumentoFirmado;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.ResultadoCdr;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.AmbienteSunat;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.EstadoEnvioSunat;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.NotaElectronica;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.TipoNotaElectronica;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.config.SunatProperties;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.persistence.NotaElectronicaRepository;
import pe.com.proveperu.sgc.security.application.exception.OperacionNoPermitidaException;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;
import pe.com.proveperu.sgc.security.infrastructure.persistence.UsuarioRepository;
import pe.com.proveperu.sgc.venta.domain.model.TipoComprobanteVenta;

@Service
@RequiredArgsConstructor
public class NotaElectronicaPersistenceService {

    private static final BigDecimal IGV_FACTOR = new BigDecimal("1.18");
    private static final Set<String> MOTIVOS_CREDITO = Set.of("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12", "13");
    private static final Set<String> MOTIVOS_DEBITO = Set.of("01", "02", "03", "10");

    private final NotaElectronicaRepository repository;
    private final ComprobanteRepository comprobanteRepository;
    private final EmpresaRepository empresaRepository;
    private final UsuarioRepository usuarioRepository;
    private final GeneradorNotaElectronicaUblService generador;
    private final DocumentoElectronicoService documentoService;
    private final SunatProperties properties;
    private final CorrelativoComprobanteService correlativoService;

    @Transactional
    public NotaElectronicaResponse crear(Long idComprobante, NotaElectronicaCrearRequest request, String login) {
        Comprobante origen = comprobanteRepository.findDetalleById(idComprobante)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el comprobante original"));
        validar(origen, request);
        var usuario = usuarioRepository.findByUsuarioLoginIgnoreCase(login)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario autenticado"));
        NotaElectronica nota = new NotaElectronica();
        nota.setComprobanteOrigen(origen);
        nota.setTipo(request.tipo());
        var numeracion = correlativoService.siguiente(
            origen.getVenta().getSede().getIdEmpresa(),
            origen.getVenta().getSede().getId(),
            properties.getAmbiente(),
            tipoNumeracion(origen, request.tipo())
        );
        nota.setSerie(numeracion.serie());
        nota.setNumero(numeracion.numero());
        nota.setCodigoMotivo(request.codigoMotivo());
        nota.setDescripcionMotivo(request.descripcionMotivo().strip());
        nota.setFechaEmision(Instant.now());
        BigDecimal total = request.total().setScale(2, RoundingMode.HALF_UP);
        BigDecimal subtotal = total.divide(IGV_FACTOR, 2, RoundingMode.HALF_UP);
        nota.setSubtotal(subtotal); nota.setIgv(total.subtract(subtotal)); nota.setTotal(total);
        nota.setUsuario(usuario); nota.setAmbiente(properties.getAmbiente()); nota.setEstado(EstadoEnvioSunat.GENERADO);
        Empresa empresa = empresa(origen);
        DocumentoFirmado documento = documentoService.preparar(generador.generar(nota, empresa));
        nota.setNombreArchivo(documento.nombreBase()); nota.setHashXml(documento.hashSha256());
        nota.setXmlFirmado(documento.xmlFirmado()); nota.setZipEnviado(documento.zip());
        return NotaElectronicaResponse.from(repository.saveAndFlush(nota));
    }

    @Transactional(readOnly = true)
    public List<NotaElectronicaResponse> listar(Long idComprobante) {
        comprobanteRepository.findById(idComprobante).orElseThrow(() -> new RecursoNoEncontradoException("No existe el comprobante original"));
        return repository.findByComprobanteOrigenIdOrderByFechaEmisionDesc(idComprobante).stream().map(NotaElectronicaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public NotaElectronicaResponse detalle(Long id) { return NotaElectronicaResponse.from(nota(id)); }

    @Transactional
    public NotaPreparada marcarEnviando(Long id) {
        NotaElectronica nota = repository.findForUpdateById(id).orElseThrow(() -> new RecursoNoEncontradoException("No existe la nota electrónica"));
        validarAmbiente(nota.getAmbiente());
        if (nota.getEstado().aceptado()) return NotaPreparada.aceptada(NotaElectronicaResponse.from(nota));
        if (nota.getEstado() == EstadoEnvioSunat.ENVIANDO) throw new OperacionNoPermitidaException("La nota electrónica ya se está enviando");
        nota.setEstado(EstadoEnvioSunat.ENVIANDO); nota.setIntentos(nota.getIntentos() + 1); nota.setFechaUltimoIntento(Instant.now()); nota.setErrorUltimo(null);
        repository.saveAndFlush(nota);
        return new NotaPreparada(nota.getId(), empresa(nota.getComprobanteOrigen()).getRuc(), nota.getNombreArchivo() + ".zip", nota.getZipEnviado(), null);
    }

    @Transactional
    public NotaElectronicaResponse registrarCdr(Long id, byte[] cdr, ResultadoCdr resultado) {
        NotaElectronica nota = nota(id); nota.setCdrZip(cdr); nota.setCodigoRespuesta(resultado.codigo()); nota.setDescripcionRespuesta(resultado.descripcion());
        nota.setObservaciones(String.join(System.lineSeparator(), resultado.observaciones())); nota.setFechaRespuesta(Instant.now()); nota.setErrorUltimo(null);
        nota.setEstado(resultado.aceptado() ? resultado.observaciones().isEmpty() ? EstadoEnvioSunat.ACEPTADO : EstadoEnvioSunat.ACEPTADO_CON_OBSERVACIONES : EstadoEnvioSunat.RECHAZADO);
        return NotaElectronicaResponse.from(repository.saveAndFlush(nota));
    }

    @Transactional
    public void registrarError(Long id, String message) { repository.findById(id).ifPresent(nota -> { nota.setEstado(EstadoEnvioSunat.ERROR_COMUNICACION); nota.setErrorUltimo(recortar(message, 2000)); repository.saveAndFlush(nota); }); }

    @Transactional
    public void registrarRechazo(Long id, String codigo, String message) { repository.findById(id).ifPresent(nota -> { nota.setEstado(EstadoEnvioSunat.RECHAZADO); nota.setCodigoRespuesta(recortar(codigo, 20)); nota.setDescripcionRespuesta(recortar(message, 1000)); nota.setFechaRespuesta(Instant.now()); repository.saveAndFlush(nota); }); }

    @Transactional(readOnly = true)
    public ArchivoElectronico xml(Long id) { NotaElectronica nota = nota(id); return new ArchivoElectronico(nota.getNombreArchivo() + ".xml", "application/xml", nota.getXmlFirmado()); }
    @Transactional(readOnly = true)
    public ArchivoElectronico cdr(Long id) { NotaElectronica nota = nota(id); if (nota.getCdrZip() == null) throw new RecursoNoEncontradoException("La nota todavía no tiene CDR"); return new ArchivoElectronico("R-" + nota.getNombreArchivo() + ".zip", "application/zip", nota.getCdrZip()); }

    private void validar(Comprobante origen, NotaElectronicaCrearRequest request) {
        validarAmbiente(origen.getAmbiente());
        if (origen.getTipo() == TipoComprobanteVenta.NOTA_VENTA) throw new OperacionNoPermitidaException("Las notas electrónicas solo se vinculan a boletas o facturas");
        if (origen.getEstado() != EstadoComprobante.EMITIDO) throw new OperacionNoPermitidaException("El comprobante original debe estar emitido y aceptado");
        Set<String> validos = request.tipo() == TipoNotaElectronica.CREDITO ? MOTIVOS_CREDITO : MOTIVOS_DEBITO;
        if (!validos.contains(request.codigoMotivo())) throw new OperacionNoPermitidaException("El código de motivo no pertenece al catálogo SUNAT de la nota seleccionada");
        if (request.tipo() == TipoNotaElectronica.CREDITO && request.total().compareTo(origen.getTotal()) > 0) throw new OperacionNoPermitidaException("La nota de crédito no puede superar el total del comprobante original");
        if (request.tipo() == TipoNotaElectronica.CREDITO && request.codigoMotivo().equals("01") && request.total().compareTo(origen.getTotal()) != 0) throw new OperacionNoPermitidaException("La anulación de la operación debe acreditar el total del comprobante");
    }

    private Empresa empresa(Comprobante origen) { return empresaRepository.findById(origen.getVenta().getSede().getIdEmpresa()).orElseThrow(() -> new RecursoNoEncontradoException("No existe la empresa emisora")); }
    private TipoNumeracionComprobante tipoNumeracion(Comprobante origen, TipoNotaElectronica tipo) {
        boolean factura = origen.getTipo() == TipoComprobanteVenta.FACTURA;
        if (tipo == TipoNotaElectronica.CREDITO) {
            return factura
                ? TipoNumeracionComprobante.NOTA_CREDITO_FACTURA
                : TipoNumeracionComprobante.NOTA_CREDITO_BOLETA;
        }
        return factura
            ? TipoNumeracionComprobante.NOTA_DEBITO_FACTURA
            : TipoNumeracionComprobante.NOTA_DEBITO_BOLETA;
    }
    private void validarAmbiente(AmbienteSunat ambiente) {
        if (ambiente != properties.getAmbiente()) {
            throw new OperacionNoPermitidaException(
                "El documento fue generado en " + ambiente
                    + " y no puede procesarse en " + properties.getAmbiente()
            );
        }
        if (ambiente == AmbienteSunat.PRODUCCION && !properties.isProductionEnabled()) {
            throw new OperacionNoPermitidaException(
                "La numeración de producción está bloqueada por "
                    + "SUNAT_PRODUCTION_ENABLED=false"
            );
        }
    }
    private NotaElectronica nota(Long id) { return repository.findDetalleById(id).orElseThrow(() -> new RecursoNoEncontradoException("No existe la nota electrónica")); }
    private String recortar(String value, int max) { if (value == null) return null; return value.length() <= max ? value : value.substring(0, max); }

    public record NotaPreparada(Long id, String ruc, String nombreZip, byte[] zip, NotaElectronicaResponse aceptada) { static NotaPreparada aceptada(NotaElectronicaResponse response) { return new NotaPreparada(response.id(), null, null, null, response); } public boolean yaAceptada() { return aceptada != null; } }
}
