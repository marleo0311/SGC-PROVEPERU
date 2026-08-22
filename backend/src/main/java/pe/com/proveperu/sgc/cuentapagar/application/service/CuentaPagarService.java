package pe.com.proveperu.sgc.cuentapagar.application.service;

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
import pe.com.proveperu.sgc.compra.domain.model.Compra;
import pe.com.proveperu.sgc.compra.domain.model.CondicionPagoCompra;
import pe.com.proveperu.sgc.compra.domain.model.EstadoCompra;
import pe.com.proveperu.sgc.configuracion.domain.model.MetodoPago;
import pe.com.proveperu.sgc.configuracion.infrastructure.persistence.MetodoPagoRepository;
import pe.com.proveperu.sgc.cuentapagar.api.dto.CuentaPagarDetalleResponse;
import pe.com.proveperu.sgc.cuentapagar.api.dto.CuentaPagarResumenResponse;
import pe.com.proveperu.sgc.cuentapagar.api.dto.MetodoPagoResponse;
import pe.com.proveperu.sgc.cuentapagar.api.dto.PagoProveedorRequest;
import pe.com.proveperu.sgc.cuentapagar.api.dto.PagoProveedorResponse;
import pe.com.proveperu.sgc.cuentapagar.domain.model.CuentaPagar;
import pe.com.proveperu.sgc.cuentapagar.domain.model.EstadoCuentaPagar;
import pe.com.proveperu.sgc.cuentapagar.domain.model.PagoProveedor;
import pe.com.proveperu.sgc.cuentapagar.infrastructure.persistence.CuentaPagarRepository;
import pe.com.proveperu.sgc.cuentapagar.infrastructure.persistence.PagoProveedorRepository;
import pe.com.proveperu.sgc.security.application.exception.OperacionNoPermitidaException;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;
import pe.com.proveperu.sgc.security.domain.model.EstadoUsuario;
import pe.com.proveperu.sgc.security.domain.model.Usuario;
import pe.com.proveperu.sgc.security.infrastructure.persistence.UsuarioRepository;
import pe.com.proveperu.sgc.shared.api.dto.PaginaResponse;
import pe.com.proveperu.sgc.shared.application.exception.ReglaNegocioException;
import pe.com.proveperu.sgc.shared.application.exception.SolicitudInvalidaException;

@Service
@RequiredArgsConstructor
public class CuentaPagarService {

    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Lima");
    private static final List<EstadoCuentaPagar> ESTADOS_PENDIENTES = List.of(
        EstadoCuentaPagar.PENDIENTE,
        EstadoCuentaPagar.PARCIAL
    );

    private final CuentaPagarRepository cuentaRepository;
    private final PagoProveedorRepository pagoRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public PaginaResponse<CuentaPagarResumenResponse> listar(
        Long idProveedor,
        EstadoCuentaPagar estado,
        LocalDate desdeVencimiento,
        LocalDate hastaVencimiento,
        Pageable pageable
    ) {
        validarRango(desdeVencimiento, hastaVencimiento);
        actualizarVencidas();
        Specification<CuentaPagar> filtros = crearFiltros(
            idProveedor,
            estado,
            desdeVencimiento,
            hastaVencimiento
        );
        Page<CuentaPagarResumenResponse> pagina = cuentaRepository
            .findAll(filtros, pageable)
            .map(CuentaPagarResumenResponse::from);
        return PaginaResponse.from(pagina);
    }

    @Transactional
    public PaginaResponse<CuentaPagarResumenResponse> listarVencidas(Pageable pageable) {
        return listar(null, EstadoCuentaPagar.VENCIDO, null, null, pageable);
    }

    @Transactional
    public CuentaPagarDetalleResponse obtener(Long id) {
        actualizarVencidas();
        return detalle(buscarCuenta(id));
    }

    @Transactional
    public CuentaPagarResumenResponse actualizarVencimiento(
        Long id,
        LocalDate fechaVencimiento
    ) {
        CuentaPagar cuenta = buscarCuentaParaActualizar(id);
        if (cuenta.getEstado() == EstadoCuentaPagar.PAGADO
            || cuenta.getEstado() == EstadoCuentaPagar.ANULADO) {
            throw new OperacionNoPermitidaException(
                "Una cuenta " + cuenta.getEstado() + " no admite cambiar su vencimiento"
            );
        }
        cuenta.setFechaVencimiento(fechaVencimiento);
        cuenta.setEstado(calcularEstado(cuenta));
        return CuentaPagarResumenResponse.from(cuentaRepository.saveAndFlush(cuenta));
    }

    @Transactional
    public CuentaPagarDetalleResponse registrarPago(
        Long id,
        PagoProveedorRequest request,
        String usuarioLogin
    ) {
        CuentaPagar cuenta = buscarCuentaParaActualizar(id);
        if (cuenta.getEstado() == EstadoCuentaPagar.PAGADO
            || cuenta.getEstado() == EstadoCuentaPagar.ANULADO) {
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

        MetodoPago metodoPago = metodoPagoRepository.findById(request.idMetodoPago())
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe el método de pago solicitado"
            ));
        if (!"ACTIVO".equalsIgnoreCase(metodoPago.getEstado())) {
            throw new OperacionNoPermitidaException(
                "El método de pago seleccionado no está activo"
            );
        }
        Usuario usuario = buscarUsuarioActivo(usuarioLogin);

        PagoProveedor pago = new PagoProveedor();
        pago.setCuentaPagar(cuenta);
        pago.setMetodoPago(metodoPago);
        pago.setUsuario(usuario);
        pago.setMonto(monto);
        pago.setReferencia(normalizarTexto(request.referencia()));
        pagoRepository.saveAndFlush(pago);

        cuenta.setImportePagado(cuenta.getImportePagado().add(monto));
        cuenta.setSaldoPendiente(cuenta.getSaldoPendiente().subtract(monto));
        cuenta.setEstado(calcularEstado(cuenta));
        cuentaRepository.saveAndFlush(cuenta);
        return detalle(cuenta);
    }

    @Transactional(readOnly = true)
    public List<MetodoPagoResponse> listarMetodosPago() {
        return metodoPagoRepository.findAllByEstadoIgnoreCaseOrderByNombreAsc("ACTIVO")
            .stream()
            .map(MetodoPagoResponse::from)
            .toList();
    }

    @Transactional
    public void generarParaCompra(Compra compra) {
        if (compra.getEstado() != EstadoCompra.RECIBIDA
            || compra.getCondicionPago() == CondicionPagoCompra.CONTADO
            || cuentaRepository.findByCompraId(compra.getId()).isPresent()) {
            return;
        }

        BigDecimal total = compra.getTotal().setScale(2, RoundingMode.UNNECESSARY);
        CuentaPagar cuenta = new CuentaPagar();
        cuenta.setCompra(compra);
        cuenta.setTotal(total);
        cuenta.setImportePagado(BigDecimal.ZERO.setScale(2));
        cuenta.setSaldoPendiente(total);
        cuenta.setEstado(EstadoCuentaPagar.PENDIENTE);
        cuentaRepository.saveAndFlush(cuenta);
    }

    private CuentaPagarDetalleResponse detalle(CuentaPagar cuenta) {
        List<PagoProveedorResponse> pagos = pagoRepository
            .findAllByCuentaPagarIdOrderByFechaHoraDescIdDesc(cuenta.getId())
            .stream()
            .map(PagoProveedorResponse::from)
            .toList();
        return new CuentaPagarDetalleResponse(
            CuentaPagarResumenResponse.from(cuenta),
            pagos
        );
    }

    private Specification<CuentaPagar> crearFiltros(
        Long idProveedor,
        EstadoCuentaPagar estado,
        LocalDate desdeVencimiento,
        LocalDate hastaVencimiento
    ) {
        return (root, query, builder) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (idProveedor != null) {
                condiciones.add(builder.equal(
                    root.get("compra").get("proveedor").get("id"),
                    idProveedor
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

    private EstadoCuentaPagar calcularEstado(CuentaPagar cuenta) {
        if (cuenta.getSaldoPendiente().compareTo(BigDecimal.ZERO) == 0) {
            return EstadoCuentaPagar.PAGADO;
        }
        if (cuenta.getFechaVencimiento() != null
            && cuenta.getFechaVencimiento().isBefore(hoy())) {
            return EstadoCuentaPagar.VENCIDO;
        }
        return cuenta.getImportePagado().compareTo(BigDecimal.ZERO) > 0
            ? EstadoCuentaPagar.PARCIAL
            : EstadoCuentaPagar.PENDIENTE;
    }

    private void actualizarVencidas() {
        cuentaRepository.marcarVencidas(
            hoy(),
            EstadoCuentaPagar.VENCIDO,
            ESTADOS_PENDIENTES
        );
    }

    private CuentaPagar buscarCuenta(Long id) {
        return cuentaRepository.findDetalleById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la cuenta por pagar solicitada"
            ));
    }

    private CuentaPagar buscarCuentaParaActualizar(Long id) {
        return cuentaRepository.findForUpdate(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la cuenta por pagar solicitada"
            ));
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
