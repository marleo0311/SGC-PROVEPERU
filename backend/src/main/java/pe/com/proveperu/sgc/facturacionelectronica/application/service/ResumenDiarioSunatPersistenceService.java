package pe.com.proveperu.sgc.facturacionelectronica.application.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.comprobante.domain.model.Comprobante;
import pe.com.proveperu.sgc.comprobante.domain.model.EstadoComprobante;
import pe.com.proveperu.sgc.comprobante.infrastructure.persistence.ComprobanteRepository;
import pe.com.proveperu.sgc.configuracion.domain.model.Empresa;
import pe.com.proveperu.sgc.configuracion.infrastructure.persistence.EmpresaRepository;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.ResumenDiarioSunatResponse;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.ArchivoElectronico;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.DocumentoFirmado;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.ResultadoCdr;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.AmbienteSunat;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.EnvioSunat;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.EstadoResumenDiarioSunat;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.ResumenDiarioSunat;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.config.SunatProperties;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.persistence.ResumenDiarioSunatRepository;
import pe.com.proveperu.sgc.security.application.exception.OperacionNoPermitidaException;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;
import pe.com.proveperu.sgc.shared.application.exception.ReglaNegocioException;
import pe.com.proveperu.sgc.venta.domain.model.TipoComprobanteVenta;

@Service
public class ResumenDiarioSunatPersistenceService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final int MAX_BOLETAS_POR_RESUMEN = 500;

    private final ComprobanteRepository comprobanteRepository;
    private final EmpresaRepository empresaRepository;
    private final ResumenDiarioSunatRepository resumenRepository;
    private final GeneradorResumenDiarioUblService generadorService;
    private final DocumentoElectronicoService documentoService;
    private final CorrelativoResumenDiarioService correlativoService;
    private final SunatProperties properties;

    public ResumenDiarioSunatPersistenceService(
        ComprobanteRepository comprobanteRepository,
        EmpresaRepository empresaRepository,
        ResumenDiarioSunatRepository resumenRepository,
        GeneradorResumenDiarioUblService generadorService,
        DocumentoElectronicoService documentoService,
        CorrelativoResumenDiarioService correlativoService,
        SunatProperties properties
    ) {
        this.comprobanteRepository = comprobanteRepository;
        this.empresaRepository = empresaRepository;
        this.resumenRepository = resumenRepository;
        this.generadorService = generadorService;
        this.documentoService = documentoService;
        this.correlativoService = correlativoService;
        this.properties = properties;
    }

    @Transactional
    public List<ResumenDiarioSunatResponse> preparar(LocalDate fechaDocumentos) {
        LocalDate hoy = LocalDate.now(LIMA);
        validarFecha(fechaDocumentos, hoy);
        AmbienteSunat ambiente = properties.getAmbiente();
        int primerCorrelativo = correlativoService.siguiente(ambiente, fechaDocumentos);
        Instant desde = fechaDocumentos.atStartOfDay(LIMA).toInstant();
        Instant hasta = fechaDocumentos.plusDays(1).atStartOfDay(LIMA).toInstant();
        List<Comprobante> encontrados = comprobanteRepository.findParaResumenDiario(
            TipoComprobanteVenta.BOLETA,
            desde,
            hasta
        );
        Set<Long> ids = encontrados.stream().map(Comprobante::getId)
            .collect(java.util.stream.Collectors.toSet());
        Set<Long> incluidos = ids.isEmpty()
            ? Set.of()
            : resumenRepository.findComprobantesIncluidos(ambiente, ids);
        List<Comprobante> pendientes = encontrados.stream()
            .filter(comprobante -> !incluidos.contains(comprobante.getId()))
            .filter(comprobante -> !aceptadoIndividualmenteEnAmbiente(comprobante, ambiente))
            .toList();
        if (pendientes.isEmpty()) {
            throw new ReglaNegocioException(
                "No hay boletas pendientes para el resumen diario de " + fechaDocumentos
            );
        }

        Long idEmpresa = empresaUnica(pendientes);
        Empresa empresa = empresaRepository.findById(idEmpresa)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la empresa emisora de las boletas"
            ));
        List<ResumenDiarioSunatResponse> resultado = new ArrayList<>();
        for (int inicio = 0, bloque = 0; inicio < pendientes.size(); inicio += MAX_BOLETAS_POR_RESUMEN, bloque++) {
            int fin = Math.min(inicio + MAX_BOLETAS_POR_RESUMEN, pendientes.size());
            List<Comprobante> boletas = List.copyOf(pendientes.subList(inicio, fin));
            int correlativo = bloque == 0
                ? primerCorrelativo
                : correlativoService.siguiente(ambiente, fechaDocumentos);
            DocumentoFirmado documento = documentoService.preparar(
                generadorService.generar(boletas, empresa, fechaDocumentos, hoy, correlativo)
            );
            ResumenDiarioSunat resumen = nuevoResumen(
                ambiente,
                fechaDocumentos,
                hoy,
                correlativo,
                documento,
                boletas
            );
            boletas.forEach(boleta -> boleta.setEstado(EstadoComprobante.PENDIENTE_ENVIO));
            resultado.add(ResumenDiarioSunatResponse.from(resumenRepository.saveAndFlush(resumen)));
        }
        return List.copyOf(resultado);
    }

    @Transactional(readOnly = true)
    public List<ResumenDiarioSunatResponse> listar(LocalDate fechaDocumentos) {
        List<ResumenDiarioSunat> resumenes = fechaDocumentos == null
            ? resumenRepository.findAllByOrderByFechaDocumentosDescCorrelativoDesc()
            : resumenRepository.findByFechaDocumentosOrderByCorrelativoDesc(fechaDocumentos);
        return resumenes.stream().map(ResumenDiarioSunatResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ResumenDiarioSunatResponse detalle(Long id) {
        return ResumenDiarioSunatResponse.from(resumen(id));
    }

    @Transactional
    public ResumenPreparado marcarEnviando(Long id) {
        ResumenDiarioSunat resumen = resumenRepository.findForUpdateById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe el resumen diario solicitado"
            ));
        if (resumen.getEstado().aceptado()) {
            return ResumenPreparado.aceptado(ResumenDiarioSunatResponse.from(resumen));
        }
        if (resumen.getEstado() == EstadoResumenDiarioSunat.ENVIANDO) {
            throw new OperacionNoPermitidaException("El resumen ya se encuentra en proceso de envío");
        }
        if (resumen.getTicket() != null && !resumen.getTicket().isBlank()) {
            throw new OperacionNoPermitidaException(
                "El resumen ya tiene ticket SUNAT; consulta su estado en lugar de reenviarlo"
            );
        }
        resumen.setEstado(EstadoResumenDiarioSunat.ENVIANDO);
        resumen.setIntentosEnvio(resumen.getIntentosEnvio() + 1);
        resumen.setFechaUltimoIntento(Instant.now());
        resumen.setErrorUltimo(null);
        resumenRepository.saveAndFlush(resumen);
        Empresa empresa = empresa(resumen);
        return new ResumenPreparado(
            resumen.getId(),
            empresa.getRuc(),
            resumen.getNombreArchivo() + ".zip",
            resumen.getZipEnviado(),
            null
        );
    }

    @Transactional
    public ResumenDiarioSunatResponse registrarTicket(Long id, String ticket) {
        ResumenDiarioSunat resumen = bloqueado(id);
        resumen.setTicket(ticket);
        resumen.setEstado(EstadoResumenDiarioSunat.TICKET_RECIBIDO);
        resumen.setErrorUltimo(null);
        return ResumenDiarioSunatResponse.from(resumenRepository.saveAndFlush(resumen));
    }

    @Transactional
    public ConsultaTicket marcarConsultando(Long id) {
        ResumenDiarioSunat resumen = bloqueado(id);
        if (resumen.getEstado().aceptado() || resumen.getEstado() == EstadoResumenDiarioSunat.RECHAZADO) {
            return ConsultaTicket.terminada(ResumenDiarioSunatResponse.from(resumen));
        }
        if (resumen.getTicket() == null || resumen.getTicket().isBlank()) {
            throw new OperacionNoPermitidaException("Primero envía el resumen para obtener un ticket SUNAT");
        }
        resumen.setConsultasEstado(resumen.getConsultasEstado() + 1);
        resumen.setFechaUltimaConsulta(Instant.now());
        resumen.setErrorUltimo(null);
        resumenRepository.saveAndFlush(resumen);
        return new ConsultaTicket(
            resumen.getId(),
            empresa(resumen).getRuc(),
            resumen.getTicket(),
            null
        );
    }

    @Transactional
    public ResumenDiarioSunatResponse registrarProcesando(Long id, String codigo, String mensaje) {
        ResumenDiarioSunat resumen = bloqueado(id);
        resumen.setEstado(EstadoResumenDiarioSunat.PROCESANDO);
        resumen.setCodigoEstadoTicket(codigo);
        resumen.setDescripcionRespuesta(recortarOpcional(mensaje, 1000));
        resumen.setErrorUltimo(null);
        return ResumenDiarioSunatResponse.from(resumenRepository.saveAndFlush(resumen));
    }

    @Transactional
    public ResumenDiarioSunatResponse registrarCdr(
        Long id,
        String codigoEstadoTicket,
        byte[] cdrZip,
        ResultadoCdr resultado
    ) {
        ResumenDiarioSunat resumen = bloqueado(id);
        resumen.setCdrZip(cdrZip);
        resumen.setCodigoEstadoTicket(codigoEstadoTicket);
        resumen.setCodigoRespuesta(resultado.codigo());
        resumen.setDescripcionRespuesta(resultado.descripcion());
        resumen.setObservaciones(String.join(System.lineSeparator(), resultado.observaciones()));
        resumen.setFechaRespuesta(Instant.now());
        resumen.setErrorUltimo(null);
        resumen.setEstado(resultado.aceptado()
            ? resultado.observaciones().isEmpty()
                ? EstadoResumenDiarioSunat.ACEPTADO
                : EstadoResumenDiarioSunat.ACEPTADO_CON_OBSERVACIONES
            : EstadoResumenDiarioSunat.RECHAZADO);
        EstadoComprobante estado = resultado.aceptado()
            ? EstadoComprobante.EMITIDO
            : EstadoComprobante.PENDIENTE_ENVIO;
        resumen.getComprobantes().forEach(comprobante -> comprobante.setEstado(estado));
        return ResumenDiarioSunatResponse.from(resumenRepository.saveAndFlush(resumen));
    }

    @Transactional
    public ResumenDiarioSunatResponse registrarTerminadoSinCdr(
        Long id,
        String codigo,
        String mensaje
    ) {
        ResumenDiarioSunat resumen = bloqueado(id);
        resumen.setEstado(EstadoResumenDiarioSunat.RECHAZADO);
        resumen.setCodigoEstadoTicket(codigo);
        resumen.setCodigoRespuesta(codigo);
        resumen.setDescripcionRespuesta(recortarOpcional(mensaje, 1000));
        resumen.setFechaRespuesta(Instant.now());
        resumen.setErrorUltimo(null);
        return ResumenDiarioSunatResponse.from(resumenRepository.saveAndFlush(resumen));
    }

    @Transactional
    public void registrarError(Long id, String mensaje) {
        resumenRepository.findForUpdateById(id).ifPresent(resumen -> {
            resumen.setEstado(EstadoResumenDiarioSunat.ERROR_COMUNICACION);
            resumen.setErrorUltimo(recortar(mensaje, 2000));
            resumenRepository.saveAndFlush(resumen);
        });
    }

    @Transactional
    public void registrarRechazoSoap(Long id, String codigo, String mensaje) {
        resumenRepository.findForUpdateById(id).ifPresent(resumen -> {
            resumen.setEstado(EstadoResumenDiarioSunat.RECHAZADO);
            resumen.setCodigoRespuesta(recortarOpcional(codigo, 20));
            resumen.setDescripcionRespuesta(recortarOpcional(mensaje, 1000));
            resumen.setFechaRespuesta(Instant.now());
            resumen.setErrorUltimo(null);
            resumenRepository.saveAndFlush(resumen);
        });
    }

    @Transactional(readOnly = true)
    public ArchivoElectronico xml(Long id) {
        ResumenDiarioSunat resumen = resumen(id);
        return new ArchivoElectronico(
            resumen.getNombreArchivo() + ".xml",
            "application/xml",
            resumen.getXmlFirmado()
        );
    }

    @Transactional(readOnly = true)
    public ArchivoElectronico cdr(Long id) {
        ResumenDiarioSunat resumen = resumen(id);
        if (resumen.getCdrZip() == null || resumen.getCdrZip().length == 0) {
            throw new RecursoNoEncontradoException("El resumen diario todavía no tiene un CDR");
        }
        return new ArchivoElectronico(
            "R-" + resumen.getNombreArchivo() + ".zip",
            "application/zip",
            resumen.getCdrZip()
        );
    }

    private void validarFecha(LocalDate fecha, LocalDate hoy) {
        if (fecha.isAfter(hoy)) {
            throw new ReglaNegocioException("No se puede resumir una fecha futura");
        }
        if (hoy.isAfter(fecha.plusDays(7))) {
            throw new ReglaNegocioException(
                "La fecha excede el plazo de siete días calendario para enviar boletas"
            );
        }
    }

    private boolean aceptadoIndividualmenteEnAmbiente(
        Comprobante comprobante,
        AmbienteSunat ambiente
    ) {
        EnvioSunat envio = comprobante.getEnvioSunat();
        return envio != null && envio.getAmbiente() == ambiente && envio.getEstado().aceptado();
    }

    private Long empresaUnica(List<Comprobante> boletas) {
        Set<Long> empresas = boletas.stream()
            .map(boleta -> boleta.getVenta().getSede().getIdEmpresa())
            .collect(java.util.stream.Collectors.toSet());
        if (empresas.size() != 1) {
            throw new ReglaNegocioException(
                "Las boletas de empresas diferentes deben enviarse en resúmenes separados"
            );
        }
        return empresas.iterator().next();
    }

    private ResumenDiarioSunat nuevoResumen(
        AmbienteSunat ambiente,
        LocalDate fechaDocumentos,
        LocalDate fechaGeneracion,
        int correlativo,
        DocumentoFirmado documento,
        List<Comprobante> boletas
    ) {
        ResumenDiarioSunat resumen = new ResumenDiarioSunat();
        resumen.setAmbiente(ambiente);
        resumen.setFechaDocumentos(fechaDocumentos);
        resumen.setFechaGeneracion(fechaGeneracion);
        resumen.setCorrelativo(correlativo);
        resumen.setEstado(EstadoResumenDiarioSunat.GENERADO);
        resumen.setNombreArchivo(documento.nombreBase());
        resumen.setHashXml(documento.hashSha256());
        resumen.setXmlFirmado(documento.xmlFirmado());
        resumen.setZipEnviado(documento.zip());
        resumen.setFechaCreacion(Instant.now());
        resumen.setComprobantes(new LinkedHashSet<>(boletas));
        return resumen;
    }

    private Empresa empresa(ResumenDiarioSunat resumen) {
        Comprobante comprobante = resumen.getComprobantes().stream().findFirst()
            .orElseThrow(() -> new IllegalStateException("El resumen diario no contiene boletas"));
        return empresaRepository.findById(comprobante.getVenta().getSede().getIdEmpresa())
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la empresa emisora del resumen diario"
            ));
    }

    private ResumenDiarioSunat resumen(Long id) {
        return resumenRepository.findDetalleById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe el resumen diario solicitado"
            ));
    }

    private ResumenDiarioSunat bloqueado(Long id) {
        return resumenRepository.findForUpdateById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe el resumen diario solicitado"
            ));
    }

    private String recortar(String value, int max) {
        String normalizado = value == null || value.isBlank()
            ? "Error de comunicación sin detalle"
            : value;
        return normalizado.length() <= max ? normalizado : normalizado.substring(0, max);
    }

    private String recortarOpcional(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    public record ResumenPreparado(
        Long id,
        String ruc,
        String nombreZip,
        byte[] zip,
        ResumenDiarioSunatResponse respuestaAceptada
    ) {
        static ResumenPreparado aceptado(ResumenDiarioSunatResponse response) {
            return new ResumenPreparado(null, null, null, null, response);
        }

        boolean yaAceptado() {
            return respuestaAceptada != null;
        }
    }

    public record ConsultaTicket(
        Long id,
        String ruc,
        String ticket,
        ResumenDiarioSunatResponse respuestaTerminada
    ) {
        static ConsultaTicket terminada(ResumenDiarioSunatResponse response) {
            return new ConsultaTicket(null, null, null, response);
        }

        boolean yaTerminada() {
            return respuestaTerminada != null;
        }
    }
}
