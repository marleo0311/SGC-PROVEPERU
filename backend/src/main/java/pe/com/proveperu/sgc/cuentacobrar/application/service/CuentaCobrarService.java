package pe.com.proveperu.sgc.cuentacobrar.application.service;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.caja.application.service.CajaService;
import pe.com.proveperu.sgc.configuracion.domain.model.MetodoPago;
import pe.com.proveperu.sgc.configuracion.infrastructure.persistence.MetodoPagoRepository;
import pe.com.proveperu.sgc.cuentacobrar.api.dto.CuentaCobrarDetalleResponse;
import pe.com.proveperu.sgc.cuentacobrar.api.dto.CuentaCobrarResumenResponse;
import pe.com.proveperu.sgc.cuentacobrar.api.dto.MetodoPagoCobranzaResponse;
import pe.com.proveperu.sgc.cuentacobrar.api.dto.PagoClienteRequest;
import pe.com.proveperu.sgc.security.application.exception.OperacionNoPermitidaException;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;
import pe.com.proveperu.sgc.security.domain.model.EstadoUsuario;
import pe.com.proveperu.sgc.security.domain.model.Usuario;
import pe.com.proveperu.sgc.security.infrastructure.persistence.UsuarioRepository;
import pe.com.proveperu.sgc.shared.api.dto.PaginaResponse;
import pe.com.proveperu.sgc.shared.application.exception.ReglaNegocioException;
import pe.com.proveperu.sgc.shared.application.exception.SolicitudInvalidaException;
import pe.com.proveperu.sgc.venta.api.dto.PagoClienteResponse;
import pe.com.proveperu.sgc.venta.domain.model.CuentaCobrar;
import pe.com.proveperu.sgc.venta.domain.model.EstadoCuentaCobrar;
import pe.com.proveperu.sgc.venta.domain.model.EstadoVenta;
import pe.com.proveperu.sgc.venta.domain.model.PagoCliente;
import pe.com.proveperu.sgc.venta.infrastructure.persistence.CuentaCobrarRepository;
import pe.com.proveperu.sgc.venta.infrastructure.persistence.PagoClienteRepository;

@Service
@RequiredArgsConstructor
public class CuentaCobrarService {

    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Lima");
    private static final List<EstadoCuentaCobrar> ESTADOS_PENDIENTES = List.of(
        EstadoCuentaCobrar.PENDIENTE,
        EstadoCuentaCobrar.PARCIAL
    );

    private final CuentaCobrarRepository cuentaRepository;
    private final PagoClienteRepository pagoRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final CajaService cajaService;

    @Transactional
    public PaginaResponse<CuentaCobrarResumenResponse> listar(
        Long idCliente,
        EstadoCuentaCobrar estado,
        LocalDate desdeVencimiento,
        LocalDate hastaVencimiento,
        Pageable pageable
    ) {
        validarRango(desdeVencimiento, hastaVencimiento);
        actualizarVencidas();
        Specification<CuentaCobrar> filtros = crearFiltros(
            idCliente,
            estado,
            desdeVencimiento,
            hastaVencimiento
        );
        Page<CuentaCobrarResumenResponse> pagina = cuentaRepository
            .findAll(filtros, pageable)
            .map(CuentaCobrarResumenResponse::from);
        return PaginaResponse.from(pagina);
    }

    @Transactional
    public PaginaResponse<CuentaCobrarResumenResponse> listarVencidas(
        Pageable pageable
    ) {
        return listar(null, EstadoCuentaCobrar.VENCIDO, null, null, pageable);
    }

    @Transactional
    public CuentaCobrarDetalleResponse obtener(Long id) {
        actualizarVencidas();
        return detalle(buscarCuenta(id));
    }

    @Transactional
    public CuentaCobrarResumenResponse actualizarVencimiento(
        Long id,
        LocalDate fechaVencimiento
    ) {
        CuentaCobrar cuenta = buscarCuentaParaActualizar(id);
        if (cuenta.getEstado() == EstadoCuentaCobrar.PAGADO
            || cuenta.getEstado() == EstadoCuentaCobrar.ANULADO) {
            throw new OperacionNoPermitidaException(
                "Una cuenta " + cuenta.getEstado()
                    + " no admite cambiar su vencimiento"
            );
        }
        cuenta.setFechaVencimiento(fechaVencimiento);
        cuenta.setEstado(calcularEstado(cuenta));
        return CuentaCobrarResumenResponse.from(
            cuentaRepository.saveAndFlush(cuenta)
        );
    }

    @Transactional
    public CuentaCobrarDetalleResponse registrarPago(
        Long id,
        PagoClienteRequest request,
        String usuarioLogin
    ) {
        CuentaCobrar cuenta = buscarCuentaParaActualizar(id);
        if (cuenta.getEstado() == EstadoCuentaCobrar.PAGADO
            || cuenta.getEstado() == EstadoCuentaCobrar.ANULADO
            || cuenta.getVenta().getEstado() == EstadoVenta.ANULADA) {
            throw new OperacionNoPermitidaException(
                "La cuenta " + cuenta.getEstado() + " no admite nuevos pagos"
            );
        }

        BigDecimal monto = normalizarMonto(request.monto());
        if (monto.compareTo(cuenta.getSaldoPendiente()) > 0) {
            throw new ReglaNegocioException(
                "El monto supera el saldo pendiente: "
                    + cuenta.getSaldoPendiente().toPlainString()
            );
        }

        MetodoPago metodoPago = buscarMetodoPagoActivo(request.idMetodoPago());
        Usuario usuario = buscarUsuarioActivo(usuarioLogin);
        PagoCliente pago = new PagoCliente();
        pago.setVenta(cuenta.getVenta());
        pago.setCuentaCobrar(cuenta);
        pago.setMetodoPago(metodoPago);
        pago.setUsuario(usuario);
        pago.setMonto(monto);
        pago.setReferencia(normalizarTexto(request.referencia()));
        pago = pagoRepository.saveAndFlush(pago);
        cajaService.registrarIngresoCobranza(pago, usuario);

        cuenta.setImportePagado(cuenta.getImportePagado().add(monto));
        cuenta.setSaldoPendiente(cuenta.getSaldoPendiente().subtract(monto));
        cuenta.setEstado(calcularEstado(cuenta));
        cuentaRepository.saveAndFlush(cuenta);
        return detalle(cuenta);
    }

    @Transactional(readOnly = true)
    public List<MetodoPagoCobranzaResponse> listarMetodosPago() {
        return metodoPagoRepository.findAllByEstadoIgnoreCaseOrderByNombreAsc("ACTIVO")
            .stream()
            .map(MetodoPagoCobranzaResponse::from)
            .toList();
    }

    private CuentaCobrarDetalleResponse detalle(CuentaCobrar cuenta) {
        List<PagoClienteResponse> pagos = pagoRepository
            .findAllByCuentaCobrarIdOrderByFechaHoraDescIdDesc(cuenta.getId())
            .stream()
            .map(PagoClienteResponse::from)
            .toList();
        return new CuentaCobrarDetalleResponse(
            CuentaCobrarResumenResponse.from(cuenta),
            pagos
        );
    }

    private Specification<CuentaCobrar> crearFiltros(
        Long idCliente,
        EstadoCuentaCobrar estado,
        LocalDate desdeVencimiento,
        LocalDate hastaVencimiento
    ) {
        return (root, query, builder) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (idCliente != null) {
                condiciones.add(builder.equal(
                    root.get("venta").get("cliente").get("id"),
                    idCliente
                ));
            }
            if (estado != null) {
                condiciones.add(builder.equal(root.get("estado"), estado));
            }
            if (desdeVencimiento != null) {
                condiciones.add(builder.greaterThanOrEqualTo(
                    root.get("fechaVencimiento"),
                    desdeVencimiento
                ));
            }
            if (hastaVencimiento != null) {
                condiciones.add(builder.lessThanOrEqualTo(
                    root.get("fechaVencimiento"),
                    hastaVencimiento
                ));
            }
            return builder.and(condiciones.toArray(Predicate[]::new));
        };
    }

    private EstadoCuentaCobrar calcularEstado(CuentaCobrar cuenta) {
        if (cuenta.getSaldoPendiente().compareTo(BigDecimal.ZERO) == 0) {
            return EstadoCuentaCobrar.PAGADO;
        }
        if (cuenta.getFechaVencimiento() != null
            && cuenta.getFechaVencimiento().isBefore(hoy())) {
            return EstadoCuentaCobrar.VENCIDO;
        }
        return cuenta.getImportePagado().compareTo(BigDecimal.ZERO) > 0
            ? EstadoCuentaCobrar.PARCIAL
            : EstadoCuentaCobrar.PENDIENTE;
    }

    private void actualizarVencidas() {
        cuentaRepository.marcarVencidas(
            hoy(),
            EstadoCuentaCobrar.VENCIDO,
            ESTADOS_PENDIENTES
        );
    }

    private CuentaCobrar buscarCuenta(Long id) {
        return cuentaRepository.findDetalleById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la cuenta por cobrar solicitada"
            ));
    }

    private CuentaCobrar buscarCuentaParaActualizar(Long id) {
        return cuentaRepository.findForUpdate(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la cuenta por cobrar solicitada"
            ));
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

    private BigDecimal normalizarMonto(BigDecimal monto) {
        try {
            return monto.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new SolicitudInvalidaException(
                "El monto admite como máximo 2 decimales"
            );
        }
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new SolicitudInvalidaException(
                "La fecha inicial no puede ser posterior a la fecha final"
            );
        }
    }

    private LocalDate hoy() {
        return LocalDate.now(ZONA_NEGOCIO);
    }

    private String normalizarTexto(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }
}
