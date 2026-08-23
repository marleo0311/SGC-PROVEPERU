package pe.com.proveperu.sgc.caja.application.service;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.caja.api.dto.AperturaCajaRequest;
import pe.com.proveperu.sgc.caja.api.dto.CajaResponse;
import pe.com.proveperu.sgc.caja.api.dto.CierreCajaRequest;
import pe.com.proveperu.sgc.caja.api.dto.MovimientoCajaRequest;
import pe.com.proveperu.sgc.caja.api.dto.MovimientoCajaResponse;
import pe.com.proveperu.sgc.caja.api.dto.MetodoPagoCajaResponse;
import pe.com.proveperu.sgc.caja.api.dto.ResumenCajaResponse;
import pe.com.proveperu.sgc.caja.api.dto.ResumenMetodoPagoResponse;
import pe.com.proveperu.sgc.caja.api.dto.SesionCajaResponse;
import pe.com.proveperu.sgc.caja.domain.model.Caja;
import pe.com.proveperu.sgc.caja.domain.model.ConceptoMovimientoCaja;
import pe.com.proveperu.sgc.caja.domain.model.EstadoCaja;
import pe.com.proveperu.sgc.caja.domain.model.EstadoSesionCaja;
import pe.com.proveperu.sgc.caja.domain.model.MovimientoCaja;
import pe.com.proveperu.sgc.caja.domain.model.SesionCaja;
import pe.com.proveperu.sgc.caja.domain.model.TipoMovimientoCaja;
import pe.com.proveperu.sgc.caja.infrastructure.persistence.CajaRepository;
import pe.com.proveperu.sgc.caja.infrastructure.persistence.MovimientoCajaRepository;
import pe.com.proveperu.sgc.caja.infrastructure.persistence.SesionCajaRepository;
import pe.com.proveperu.sgc.configuracion.domain.model.MetodoPago;
import pe.com.proveperu.sgc.configuracion.infrastructure.persistence.MetodoPagoRepository;
import pe.com.proveperu.sgc.security.application.exception.OperacionNoPermitidaException;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;
import pe.com.proveperu.sgc.security.domain.model.EstadoUsuario;
import pe.com.proveperu.sgc.security.domain.model.Usuario;
import pe.com.proveperu.sgc.security.infrastructure.persistence.UsuarioRepository;
import pe.com.proveperu.sgc.shared.api.dto.PaginaResponse;
import pe.com.proveperu.sgc.shared.application.exception.SolicitudInvalidaException;
import pe.com.proveperu.sgc.venta.domain.model.PagoCliente;
import pe.com.proveperu.sgc.venta.domain.model.Venta;

@Service
@RequiredArgsConstructor
public class CajaService {

    private static final int ESCALA_DINERO = 2;
    private static final BigDecimal CERO = BigDecimal.ZERO.setScale(ESCALA_DINERO);
    private static final String EFECTIVO = "EFECTIVO";

    private final CajaRepository cajaRepository;
    private final SesionCajaRepository sesionRepository;
    private final MovimientoCajaRepository movimientoRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<CajaResponse> listarCajas() {
        return cajaRepository.findAllByEstadoOrderBySedeNombreAscNombreAsc(
            EstadoCaja.ACTIVO
        ).stream().map(CajaResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<MetodoPagoCajaResponse> listarMetodosPago() {
        return metodoPagoRepository.findAllByEstadoIgnoreCaseOrderByNombreAsc("ACTIVO")
            .stream()
            .map(MetodoPagoCajaResponse::from)
            .toList();
    }

    @Transactional
    public SesionCajaResponse abrir(
        Long idCaja,
        AperturaCajaRequest request,
        String usuarioLogin
    ) {
        Caja caja = buscarCajaActiva(idCaja);
        Usuario usuario = buscarUsuarioActivo(usuarioLogin);
        if (sesionRepository.existsByCajaIdAndEstado(
            idCaja,
            EstadoSesionCaja.ABIERTA
        )) {
            throw new OperacionNoPermitidaException(
                "La caja ya tiene una sesión abierta"
            );
        }
        if (sesionRepository.existsByUsuarioAperturaIdAndEstado(
            usuario.getId(),
            EstadoSesionCaja.ABIERTA
        )) {
            throw new OperacionNoPermitidaException(
                "El usuario ya tiene una sesión de caja abierta"
            );
        }

        SesionCaja sesion = new SesionCaja();
        sesion.setCaja(caja);
        sesion.setUsuarioApertura(usuario);
        sesion.setSaldoInicial(normalizarDinero(
            request.saldoInicial(),
            "El saldo inicial"
        ));
        sesion.setEstado(EstadoSesionCaja.ABIERTA);
        return SesionCajaResponse.from(sesionRepository.saveAndFlush(sesion));
    }

    @Transactional(readOnly = true)
    public SesionCajaResponse obtenerSesionActiva(Long idCaja) {
        buscarCajaActiva(idCaja);
        SesionCaja sesion = sesionRepository.findByCajaIdAndEstado(
            idCaja,
            EstadoSesionCaja.ABIERTA
        ).orElseThrow(() -> new RecursoNoEncontradoException(
            "La caja no tiene una sesión abierta"
        ));
        return SesionCajaResponse.from(sesion);
    }

    @Transactional
    public MovimientoCajaResponse registrarMovimientoManual(
        Long idSesion,
        MovimientoCajaRequest request,
        String usuarioLogin
    ) {
        SesionCaja sesion = buscarSesionAbiertaParaActualizar(idSesion);
        validarConceptoManual(request.tipo(), request.concepto());
        MetodoPago metodo = buscarMetodoPagoActivo(request.idMetodoPago());
        Usuario usuario = buscarUsuarioActivo(usuarioLogin);

        MovimientoCaja movimiento = new MovimientoCaja();
        movimiento.setSesion(sesion);
        movimiento.setMetodoPago(metodo);
        movimiento.setUsuario(usuario);
        movimiento.setTipo(request.tipo());
        movimiento.setConcepto(request.concepto());
        movimiento.setImporte(normalizarDinero(request.importe(), "El importe"));
        movimiento.setReferencia(normalizarTexto(request.referencia()));
        movimiento.setObservacion(normalizarTexto(request.observacion()));
        return MovimientoCajaResponse.from(
            movimientoRepository.saveAndFlush(movimiento)
        );
    }

    @Transactional(readOnly = true)
    public PaginaResponse<MovimientoCajaResponse> listarMovimientos(
        Long idSesion,
        Instant desde,
        Instant hasta,
        Long idUsuario,
        TipoMovimientoCaja tipo,
        Long idMetodoPago,
        Long idVendedor,
        Pageable pageable
    ) {
        buscarSesion(idSesion);
        validarRango(desde, hasta);
        Page<MovimientoCajaResponse> pagina = movimientoRepository.findAll(
            crearFiltros(
                idSesion,
                desde,
                hasta,
                idUsuario,
                tipo,
                idMetodoPago,
                idVendedor
            ),
            pageable
        ).map(MovimientoCajaResponse::from);
        return PaginaResponse.from(pagina);
    }

    @Transactional(readOnly = true)
    public ResumenCajaResponse obtenerResumen(Long idSesion) {
        SesionCaja sesion = buscarSesion(idSesion);
        return construirResumen(sesion);
    }

    @Transactional
    public SesionCajaResponse cerrar(
        Long idSesion,
        CierreCajaRequest request,
        String usuarioLogin
    ) {
        SesionCaja sesion = buscarSesionAbiertaParaActualizar(idSesion);
        Usuario usuario = buscarUsuarioActivo(usuarioLogin);
        BigDecimal saldoEsperado = calcularSaldoEsperado(
            sesion,
            movimientosDe(sesion.getId())
        );
        BigDecimal saldoReal = normalizarDinero(request.saldoReal(), "El saldo real");

        sesion.setUsuarioCierre(usuario);
        sesion.setFechaHoraCierre(Instant.now());
        sesion.setSaldoEsperado(saldoEsperado);
        sesion.setSaldoReal(saldoReal);
        sesion.setDiferencia(saldoReal.subtract(saldoEsperado));
        sesion.setObservacionCierre(normalizarTexto(request.observacion()));
        sesion.setEstado(EstadoSesionCaja.CERRADA);
        return SesionCajaResponse.from(sesionRepository.saveAndFlush(sesion));
    }

    @Transactional
    public void registrarIngresoVenta(
        Venta venta,
        PagoCliente pago,
        Usuario usuario
    ) {
        registrarIngresoAutomatico(
            usuario,
            venta,
            pago,
            pago.getMetodoPago(),
            pago.getMonto(),
            ConceptoMovimientoCaja.VENTA,
            venta.getId(),
            pago.getReferencia()
        );
    }

    @Transactional
    public void registrarIngresoCobranza(
        PagoCliente pago,
        Usuario usuario
    ) {
        registrarIngresoAutomatico(
            usuario,
            pago.getVenta(),
            pago,
            pago.getMetodoPago(),
            pago.getMonto(),
            ConceptoMovimientoCaja.PAGO_CLIENTE,
            pago.getId(),
            pago.getReferencia()
        );
    }

    @Transactional
    public void registrarEgresoReembolso(
        Venta venta,
        Long idDevolucion,
        Long idReembolso,
        MetodoPago metodo,
        BigDecimal importe,
        Usuario usuario,
        String referencia
    ) {
        registrarMovimientoPostventa(
            venta,
            idReembolso,
            metodo,
            importe,
            usuario,
            referencia,
            TipoMovimientoCaja.EGRESO,
            ConceptoMovimientoCaja.REEMBOLSO,
            "Reembolso de la devolución #" + idDevolucion,
            "Debe abrir una caja antes de registrar un reembolso"
        );
    }

    @Transactional
    public void registrarIngresoCambio(
        Venta venta,
        Long idDevolucion,
        MetodoPago metodo,
        BigDecimal importe,
        Usuario usuario,
        String referencia
    ) {
        registrarMovimientoPostventa(
            venta,
            idDevolucion,
            metodo,
            importe,
            usuario,
            referencia,
            TipoMovimientoCaja.INGRESO,
            ConceptoMovimientoCaja.CAMBIO_COBRO,
            "Diferencia cobrada por el cambio de la devolución #" + idDevolucion,
            "Debe abrir una caja antes de cobrar la diferencia del cambio"
        );
    }

    @Transactional
    public void registrarEgresoCambio(
        Venta venta,
        Long idDevolucion,
        MetodoPago metodo,
        BigDecimal importe,
        Usuario usuario,
        String referencia
    ) {
        registrarMovimientoPostventa(
            venta,
            idDevolucion,
            metodo,
            importe,
            usuario,
            referencia,
            TipoMovimientoCaja.EGRESO,
            ConceptoMovimientoCaja.CAMBIO_REEMBOLSO,
            "Diferencia devuelta por el cambio de la devolución #" + idDevolucion,
            "Debe abrir una caja antes de devolver la diferencia del cambio"
        );
    }

    @Transactional
    public void registrarEgresoDescuento(
        Venta venta,
        Long idDevolucion,
        MetodoPago metodo,
        BigDecimal importe,
        Usuario usuario,
        String referencia
    ) {
        registrarMovimientoPostventa(
            venta,
            idDevolucion,
            metodo,
            importe,
            usuario,
            referencia,
            TipoMovimientoCaja.EGRESO,
            ConceptoMovimientoCaja.DESCUENTO_REEMBOLSO,
            "Importe entregado por descuento de la devolución #" + idDevolucion,
            "Debe abrir una caja antes de entregar el descuento"
        );
    }

    private void registrarMovimientoPostventa(
        Venta venta,
        Long idOrigen,
        MetodoPago metodo,
        BigDecimal importe,
        Usuario usuario,
        String referencia,
        TipoMovimientoCaja tipo,
        ConceptoMovimientoCaja concepto,
        String observacion,
        String mensajeCajaCerrada
    ) {
        SesionCaja sesion = sesionRepository.findActivaForUpdate(
            usuario.getUsuarioLogin(),
            EstadoSesionCaja.ABIERTA
        ).orElseThrow(() -> new OperacionNoPermitidaException(
            mensajeCajaCerrada
        ));
        if (!sesion.getCaja().getSede().getId().equals(venta.getSede().getId())) {
            throw new OperacionNoPermitidaException(
                "La caja abierta pertenece a una sede diferente a la venta"
            );
        }

        MovimientoCaja movimiento = new MovimientoCaja();
        movimiento.setSesion(sesion);
        movimiento.setMetodoPago(metodo);
        movimiento.setUsuario(usuario);
        movimiento.setVenta(venta);
        movimiento.setVendedor(venta.getVendedor());
        movimiento.setTipo(tipo);
        movimiento.setConcepto(concepto);
        movimiento.setIdOrigen(idOrigen);
        movimiento.setImporte(normalizarDinero(importe, "El importe"));
        movimiento.setReferencia(normalizarTexto(referencia));
        movimiento.setObservacion(observacion);
        movimientoRepository.saveAndFlush(movimiento);
    }

    private void registrarIngresoAutomatico(
        Usuario usuario,
        Venta venta,
        PagoCliente pago,
        MetodoPago metodo,
        BigDecimal importe,
        ConceptoMovimientoCaja concepto,
        Long idOrigen,
        String referencia
    ) {
        SesionCaja sesion = sesionRepository.findActivaForUpdate(
            usuario.getUsuarioLogin(),
            EstadoSesionCaja.ABIERTA
        ).orElseThrow(() -> new OperacionNoPermitidaException(
            "Debe abrir una caja antes de registrar un pago"
        ));
        if (!sesion.getCaja().getSede().getId().equals(venta.getSede().getId())) {
            throw new OperacionNoPermitidaException(
                "La caja abierta pertenece a una sede diferente a la venta"
            );
        }

        MovimientoCaja movimiento = new MovimientoCaja();
        movimiento.setSesion(sesion);
        movimiento.setMetodoPago(metodo);
        movimiento.setUsuario(usuario);
        movimiento.setVenta(venta);
        movimiento.setPagoCliente(pago);
        movimiento.setVendedor(venta.getVendedor());
        movimiento.setTipo(TipoMovimientoCaja.INGRESO);
        movimiento.setConcepto(concepto);
        movimiento.setIdOrigen(idOrigen);
        movimiento.setImporte(importe);
        movimiento.setReferencia(normalizarTexto(referencia));
        movimientoRepository.saveAndFlush(movimiento);
    }

    private ResumenCajaResponse construirResumen(SesionCaja sesion) {
        List<MovimientoCaja> movimientos = movimientosDe(sesion.getId());
        BigDecimal ingresos = CERO;
        BigDecimal egresos = CERO;
        Map<Long, AcumuladoMetodo> porMetodo = new LinkedHashMap<>();

        for (MovimientoCaja movimiento : movimientos) {
            AcumuladoMetodo acumulado = porMetodo.computeIfAbsent(
                movimiento.getMetodoPago().getId(),
                id -> new AcumuladoMetodo(movimiento.getMetodoPago())
            );
            if (movimiento.getTipo() == TipoMovimientoCaja.INGRESO) {
                ingresos = ingresos.add(movimiento.getImporte());
                acumulado.ingresos = acumulado.ingresos.add(movimiento.getImporte());
            } else {
                egresos = egresos.add(movimiento.getImporte());
                acumulado.egresos = acumulado.egresos.add(movimiento.getImporte());
            }
        }

        List<ResumenMetodoPagoResponse> metodos = porMetodo.values().stream()
            .sorted(Comparator.comparing(valor -> valor.metodo.getNombre()))
            .map(AcumuladoMetodo::toResponse)
            .toList();
        return new ResumenCajaResponse(
            SesionCajaResponse.from(sesion),
            ingresos,
            egresos,
            ingresos.subtract(egresos),
            calcularSaldoEsperado(sesion, movimientos),
            metodos
        );
    }

    private BigDecimal calcularSaldoEsperado(
        SesionCaja sesion,
        List<MovimientoCaja> movimientos
    ) {
        BigDecimal saldo = sesion.getSaldoInicial();
        for (MovimientoCaja movimiento : movimientos) {
            if (!EFECTIVO.equalsIgnoreCase(movimiento.getMetodoPago().getCodigo())) {
                continue;
            }
            saldo = movimiento.getTipo() == TipoMovimientoCaja.INGRESO
                ? saldo.add(movimiento.getImporte())
                : saldo.subtract(movimiento.getImporte());
        }
        return saldo;
    }

    private List<MovimientoCaja> movimientosDe(Long idSesion) {
        return movimientoRepository
            .findAllBySesionIdOrderByFechaHoraAscIdAsc(idSesion);
    }

    private Specification<MovimientoCaja> crearFiltros(
        Long idSesion,
        Instant desde,
        Instant hasta,
        Long idUsuario,
        TipoMovimientoCaja tipo,
        Long idMetodoPago,
        Long idVendedor
    ) {
        return (root, query, builder) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(builder.equal(root.get("sesion").get("id"), idSesion));
            if (desde != null) {
                condiciones.add(builder.greaterThanOrEqualTo(
                    root.get("fechaHora"),
                    desde
                ));
            }
            if (hasta != null) {
                condiciones.add(builder.lessThanOrEqualTo(
                    root.get("fechaHora"),
                    hasta
                ));
            }
            if (idUsuario != null) {
                condiciones.add(builder.equal(root.get("usuario").get("id"), idUsuario));
            }
            if (tipo != null) {
                condiciones.add(builder.equal(root.get("tipo"), tipo));
            }
            if (idMetodoPago != null) {
                condiciones.add(builder.equal(
                    root.get("metodoPago").get("id"),
                    idMetodoPago
                ));
            }
            if (idVendedor != null) {
                condiciones.add(builder.equal(
                    root.get("vendedor").get("id"),
                    idVendedor
                ));
            }
            return builder.and(condiciones.toArray(Predicate[]::new));
        };
    }

    private Caja buscarCajaActiva(Long idCaja) {
        Caja caja = cajaRepository.findDetalleById(idCaja)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la caja solicitada"
            ));
        if (caja.getEstado() != EstadoCaja.ACTIVO) {
            throw new OperacionNoPermitidaException("La caja no está activa");
        }
        return caja;
    }

    private SesionCaja buscarSesion(Long idSesion) {
        return sesionRepository.findDetalleById(idSesion)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la sesión de caja solicitada"
            ));
    }

    private SesionCaja buscarSesionAbiertaParaActualizar(Long idSesion) {
        SesionCaja sesion = sesionRepository.findForUpdate(idSesion)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la sesión de caja solicitada"
            ));
        if (sesion.getEstado() != EstadoSesionCaja.ABIERTA) {
            throw new OperacionNoPermitidaException(
                "La sesión de caja ya está cerrada"
            );
        }
        return sesion;
    }

    private MetodoPago buscarMetodoPagoActivo(Long idMetodoPago) {
        MetodoPago metodo = metodoPagoRepository.findById(idMetodoPago)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe el método de pago solicitado"
            ));
        if (!"ACTIVO".equalsIgnoreCase(metodo.getEstado())) {
            throw new OperacionNoPermitidaException(
                "El método de pago seleccionado no está activo"
            );
        }
        return metodo;
    }

    private Usuario buscarUsuarioActivo(String usuarioLogin) {
        Usuario usuario = usuarioRepository.findByUsuarioLoginIgnoreCase(usuarioLogin)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe el usuario autenticado"
            ));
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new OperacionNoPermitidaException(
                "El usuario autenticado no está activo"
            );
        }
        return usuario;
    }

    private void validarConceptoManual(
        TipoMovimientoCaja tipo,
        ConceptoMovimientoCaja concepto
    ) {
        boolean valido = tipo == TipoMovimientoCaja.INGRESO
            && concepto == ConceptoMovimientoCaja.INGRESO_MANUAL
            || tipo == TipoMovimientoCaja.EGRESO
            && concepto == ConceptoMovimientoCaja.EGRESO_MANUAL;
        if (!valido) {
            throw new SolicitudInvalidaException(
                "Solo se permiten INGRESO_MANUAL o EGRESO_MANUAL y deben coincidir con el tipo"
            );
        }
    }

    private void validarRango(Instant desde, Instant hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new SolicitudInvalidaException(
                "La fecha inicial no puede ser posterior a la fecha final"
            );
        }
    }

    private BigDecimal normalizarDinero(BigDecimal valor, String campo) {
        try {
            return valor.setScale(ESCALA_DINERO, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new SolicitudInvalidaException(
                campo + " admite como máximo 2 decimales"
            );
        }
    }

    private String normalizarTexto(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }

    private static final class AcumuladoMetodo {

        private final MetodoPago metodo;
        private BigDecimal ingresos = CERO;
        private BigDecimal egresos = CERO;

        private AcumuladoMetodo(MetodoPago metodo) {
            this.metodo = metodo;
        }

        private ResumenMetodoPagoResponse toResponse() {
            return new ResumenMetodoPagoResponse(
                metodo.getId(),
                metodo.getCodigo(),
                metodo.getNombre(),
                ingresos,
                egresos,
                ingresos.subtract(egresos)
            );
        }
    }
}
