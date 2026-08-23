package pe.com.proveperu.sgc.cotizacion.application.service;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.catalogo.domain.model.PrecioProducto;
import pe.com.proveperu.sgc.catalogo.domain.model.Producto;
import pe.com.proveperu.sgc.catalogo.domain.model.ProductoUnidadConversion;
import pe.com.proveperu.sgc.catalogo.domain.model.UnidadMedida;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.PrecioProductoRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.ProductoRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.ProductoUnidadConversionRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.UnidadMedidaRepository;
import pe.com.proveperu.sgc.cliente.domain.model.Cliente;
import pe.com.proveperu.sgc.cliente.domain.model.ClientePrecioEspecial;
import pe.com.proveperu.sgc.cliente.infrastructure.persistence.ClientePrecioEspecialRepository;
import pe.com.proveperu.sgc.cliente.infrastructure.persistence.ClienteRepository;
import pe.com.proveperu.sgc.cotizacion.api.dto.CotizacionDetalleRequest;
import pe.com.proveperu.sgc.cotizacion.api.dto.CotizacionDetalleResponse;
import pe.com.proveperu.sgc.cotizacion.api.dto.CotizacionGuardarRequest;
import pe.com.proveperu.sgc.cotizacion.api.dto.CotizacionResponse;
import pe.com.proveperu.sgc.cotizacion.api.dto.CotizacionResumenResponse;
import pe.com.proveperu.sgc.cotizacion.domain.model.Cotizacion;
import pe.com.proveperu.sgc.cotizacion.domain.model.DetalleCotizacion;
import pe.com.proveperu.sgc.cotizacion.domain.model.EstadoCotizacion;
import pe.com.proveperu.sgc.cotizacion.infrastructure.persistence.CotizacionRepository;
import pe.com.proveperu.sgc.inventario.domain.model.Inventario;
import pe.com.proveperu.sgc.inventario.domain.model.Sede;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.InventarioRepository;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.SedeRepository;
import pe.com.proveperu.sgc.security.application.exception.OperacionNoPermitidaException;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;
import pe.com.proveperu.sgc.security.domain.model.EstadoUsuario;
import pe.com.proveperu.sgc.security.domain.model.Usuario;
import pe.com.proveperu.sgc.security.infrastructure.persistence.UsuarioRepository;
import pe.com.proveperu.sgc.shared.api.dto.PaginaResponse;
import pe.com.proveperu.sgc.shared.application.exception.SolicitudInvalidaException;
import pe.com.proveperu.sgc.shared.application.service.CalculoTributario;

@Service
@RequiredArgsConstructor
public class CotizacionService {

    private static final int ESCALA_DINERO = 2;
    private static final int ESCALA_CANTIDAD = 3;
    private static final int MAX_ENTEROS_DINERO = 12;
    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Lima");
    private static final List<EstadoCotizacion> ESTADOS_VIGENTES = List.of(
        EstadoCotizacion.PENDIENTE,
        EstadoCotizacion.ACEPTADA
    );

    private final CotizacionRepository cotizacionRepository;
    private final ClienteRepository clienteRepository;
    private final ClientePrecioEspecialRepository precioEspecialRepository;
    private final ProductoRepository productoRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;
    private final ProductoUnidadConversionRepository conversionRepository;
    private final PrecioProductoRepository precioRepository;
    private final UsuarioRepository usuarioRepository;
    private final SedeRepository sedeRepository;
    private final InventarioRepository inventarioRepository;

    @Transactional
    public PaginaResponse<CotizacionResumenResponse> listar(
        Long idCliente,
        EstadoCotizacion estado,
        LocalDate desde,
        LocalDate hasta,
        Pageable pageable
    ) {
        validarRango(desde, hasta);
        actualizarVencidas();
        Page<CotizacionResumenResponse> pagina = cotizacionRepository.findAll(
            crearFiltros(idCliente, estado, desde, hasta),
            pageable
        ).map(CotizacionResumenResponse::from);
        return PaginaResponse.from(pagina);
    }

    @Transactional
    public CotizacionResponse obtener(Long id) {
        actualizarVencidas();
        return respuesta(buscarCotizacion(id));
    }

    @Transactional
    public CotizacionResponse crear(
        CotizacionGuardarRequest request,
        String usuarioLogin,
        boolean puedeAplicarDescuento
    ) {
        validarVencimiento(request.fechaVencimiento());
        Cliente cliente = buscarClienteActivo(request.idCliente());
        Usuario usuario = buscarUsuarioActivo(usuarioLogin);

        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setCliente(cliente);
        cotizacion.setUsuario(usuario);
        cotizacion.setEstado(EstadoCotizacion.PENDIENTE);
        aplicarDatos(cotizacion, request, puedeAplicarDescuento);
        cotizacion = cotizacionRepository.saveAndFlush(cotizacion);
        return respuesta(cotizacion);
    }

    @Transactional
    public CotizacionResponse actualizar(
        Long id,
        CotizacionGuardarRequest request,
        boolean puedeAplicarDescuento
    ) {
        actualizarVencidas();
        validarVencimiento(request.fechaVencimiento());
        Cotizacion cotizacion = buscarCotizacion(id);
        validarEditable(cotizacion);
        cotizacion.setCliente(buscarClienteActivo(request.idCliente()));
        cotizacion.getDetalles().clear();
        cotizacionRepository.flush();
        aplicarDatos(cotizacion, request, puedeAplicarDescuento);
        return respuesta(cotizacionRepository.saveAndFlush(cotizacion));
    }

    @Transactional
    public CotizacionResponse cambiarEstado(Long id, EstadoCotizacion nuevoEstado) {
        actualizarVencidas();
        if (nuevoEstado != EstadoCotizacion.ACEPTADA
            && nuevoEstado != EstadoCotizacion.RECHAZADA) {
            throw new OperacionNoPermitidaException(
                "Desde este endpoint solo se puede aceptar o rechazar una cotización"
            );
        }

        Cotizacion cotizacion = buscarCotizacion(id);
        if (cotizacion.getEstado() == nuevoEstado) {
            return respuesta(cotizacion);
        }
        if (cotizacion.getEstado() != EstadoCotizacion.PENDIENTE) {
            throw new OperacionNoPermitidaException(
                "Una cotización " + cotizacion.getEstado()
                    + " no admite este cambio de estado"
            );
        }
        cotizacion.setEstado(nuevoEstado);
        return respuesta(cotizacionRepository.saveAndFlush(cotizacion));
    }

    private void aplicarDatos(
        Cotizacion cotizacion,
        CotizacionGuardarRequest request,
        boolean puedeAplicarDescuento
    ) {
        cotizacion.setFecha(request.fecha());
        cotizacion.setFechaVencimiento(request.fechaVencimiento());
        Set<Long> productosUnicos = new HashSet<>();
        BigDecimal importeFinal = dinero(BigDecimal.ZERO);
        for (CotizacionDetalleRequest item : request.detalles()) {
            if (!productosUnicos.add(item.idProducto())) {
                throw new SolicitudInvalidaException(
                    "No se puede repetir un producto en la cotización"
                );
            }
            DetalleCotizacion detalle = crearDetalle(
                cotizacion.getCliente(),
                request.fecha(),
                item,
                puedeAplicarDescuento
            );
            cotizacion.agregarDetalle(detalle);
            importeFinal = importeFinal.add(detalle.getSubtotal());
        }
        importeFinal = validarDinero(importeFinal, "El total");
        CalculoTributario.Totales totales = CalculoTributario.desdePrecioFinal(
            importeFinal,
            request.aplicarIgv()
        );
        cotizacion.setSubtotal(totales.subtotal());
        cotizacion.setIgv(totales.igv());
        cotizacion.setTotal(totales.total());
    }

    private DetalleCotizacion crearDetalle(
        Cliente cliente,
        LocalDate fecha,
        CotizacionDetalleRequest item,
        boolean puedeAplicarDescuento
    ) {
        Producto producto = buscarProductoActivo(item.idProducto());
        UnidadMedida unidad = buscarUnidadActiva(item.idUnidadMedida());
        BigDecimal factorUnidad = factorAUnidadBase(producto, unidad);
        BigDecimal cantidad = normalizarCantidad(item.cantidad(), unidad);
        String tipoPrecio = normalizarTipoPrecio(item.tipoPrecio());
        BigDecimal precioBase = resolverPrecioBase(
            cliente,
            producto,
            tipoPrecio,
            fecha
        );
        BigDecimal precioUnitario = validarDinero(
            precioBase.multiply(factorUnidad),
            "El precio unitario"
        );
        BigDecimal descuento = validarDinero(item.descuento(), "El descuento");
        if (descuento.compareTo(BigDecimal.ZERO) > 0 && !puedeAplicarDescuento) {
            throw new OperacionNoPermitidaException(
                "No tiene permiso para aplicar descuentos en cotizaciones"
            );
        }
        BigDecimal importeBruto = validarDinero(
            cantidad.multiply(precioUnitario),
            "El importe del detalle"
        );
        if (descuento.compareTo(importeBruto) > 0) {
            throw new SolicitudInvalidaException(
                "El descuento no puede superar el importe del producto "
                    + producto.getCodigoInterno()
            );
        }

        DetalleCotizacion detalle = new DetalleCotizacion();
        detalle.setProducto(producto);
        detalle.setUnidadMedida(unidad);
        detalle.setCantidad(cantidad);
        detalle.setPrecioUnitario(precioUnitario);
        detalle.setDescuento(descuento);
        detalle.setSubtotal(importeBruto.subtract(descuento));
        return detalle;
    }

    private CotizacionResponse respuesta(Cotizacion cotizacion) {
        Sede sede = sedeRepository.findFirstByEstadoIgnoreCaseOrderByIdAsc("ACTIVO")
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe una sede activa para consultar disponibilidad"
            ));
        Map<Long, Inventario> inventarios = inventarioRepository
            .findAllBySedeIdAndProductoIdIn(
                sede.getId(),
                cotizacion.getDetalles().stream()
                    .map(detalle -> detalle.getProducto().getId())
                    .toList()
            )
            .stream()
            .collect(Collectors.toMap(
                inventario -> inventario.getProducto().getId(),
                Function.identity()
            ));

        List<CotizacionDetalleResponse> detalles = cotizacion.getDetalles().stream()
            .map(detalle -> mapearDetalle(detalle, inventarios.get(
                detalle.getProducto().getId()
            )))
            .toList();
        return new CotizacionResponse(
            CotizacionResumenResponse.from(cotizacion),
            sede.getId(),
            sede.getNombre(),
            detalles.stream().allMatch(CotizacionDetalleResponse::disponible),
            detalles,
            cotizacion.getFechaRegistro(),
            cotizacion.getFechaActualizacion()
        );
    }

    private CotizacionDetalleResponse mapearDetalle(
        DetalleCotizacion detalle,
        Inventario inventario
    ) {
        BigDecimal factor = factorAUnidadBase(
            detalle.getProducto(),
            detalle.getUnidadMedida()
        );
        BigDecimal cantidadBase = detalle.getCantidad()
            .multiply(factor)
            .setScale(ESCALA_CANTIDAD, RoundingMode.HALF_UP);
        BigDecimal stockBase = inventario == null
            ? BigDecimal.ZERO.setScale(ESCALA_CANTIDAD)
            : inventario.getStockDisponible();
        BigDecimal stockEnUnidad = stockBase.divide(
            factor,
            ESCALA_CANTIDAD,
            RoundingMode.DOWN
        );
        return new CotizacionDetalleResponse(
            detalle.getId(),
            detalle.getProducto().getId(),
            detalle.getProducto().getCodigoInterno(),
            detalle.getProducto().getNombre(),
            detalle.getUnidadMedida().getId(),
            detalle.getUnidadMedida().getCodigo(),
            detalle.getUnidadMedida().getNombre(),
            detalle.getCantidad(),
            detalle.getPrecioUnitario(),
            detalle.getDescuento(),
            detalle.getSubtotal(),
            cantidadBase,
            stockBase,
            stockEnUnidad,
            cantidadBase.compareTo(stockBase) <= 0
        );
    }

    private BigDecimal resolverPrecioBase(
        Cliente cliente,
        Producto producto,
        String tipoPrecio,
        LocalDate fecha
    ) {
        if (cliente != null) {
            List<ClientePrecioEspecial> especiales = precioEspecialRepository
                .buscarVigentes(
                    cliente.getId(),
                    producto.getId(),
                    fecha,
                    EstadoCatalogo.ACTIVO
                );
            if (!especiales.isEmpty()) {
                return especiales.getFirst().getPrecio();
            }
        }
        List<PrecioProducto> precios = precioRepository.buscarVigentes(
            producto.getId(),
            tipoPrecio,
            fecha,
            EstadoCatalogo.ACTIVO
        );
        if (precios.isEmpty()) {
            throw new OperacionNoPermitidaException(
                "No existe un precio " + tipoPrecio + " vigente para el producto "
                    + producto.getCodigoInterno()
            );
        }
        return precios.getFirst().getMonto();
    }

    private BigDecimal factorAUnidadBase(Producto producto, UnidadMedida unidad) {
        Long idBase = producto.getUnidadBase().getId();
        if (idBase.equals(unidad.getId())) {
            return BigDecimal.ONE;
        }
        return conversionRepository
            .findByProductoIdAndUnidadOrigenIdAndUnidadDestinoIdAndEstado(
                producto.getId(),
                unidad.getId(),
                idBase,
                EstadoCatalogo.ACTIVO
            )
            .map(ProductoUnidadConversion::getFactorConversion)
            .orElseGet(() -> conversionRepository
                .findByProductoIdAndUnidadOrigenIdAndUnidadDestinoIdAndEstado(
                    producto.getId(),
                    idBase,
                    unidad.getId(),
                    EstadoCatalogo.ACTIVO
                )
                .map(conversion -> BigDecimal.ONE.divide(
                    conversion.getFactorConversion(),
                    12,
                    RoundingMode.HALF_UP
                ))
                .orElseThrow(() -> new SolicitudInvalidaException(
                    "La unidad " + unidad.getCodigo()
                        + " no es la unidad base ni tiene una conversión activa para el producto "
                        + producto.getCodigoInterno()
                ))
            );
    }

    private Specification<Cotizacion> crearFiltros(
        Long idCliente,
        EstadoCotizacion estado,
        LocalDate desde,
        LocalDate hasta
    ) {
        return (root, query, builder) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (idCliente != null) {
                condiciones.add(builder.equal(root.get("cliente").get("id"), idCliente));
            }
            if (estado != null) {
                condiciones.add(builder.equal(root.get("estado"), estado));
            }
            if (desde != null) {
                condiciones.add(builder.greaterThanOrEqualTo(root.get("fecha"), desde));
            }
            if (hasta != null) {
                condiciones.add(builder.lessThanOrEqualTo(root.get("fecha"), hasta));
            }
            return builder.and(condiciones.toArray(Predicate[]::new));
        };
    }

    private Cliente buscarClienteActivo(Long idCliente) {
        if (idCliente == null) {
            return null;
        }
        Cliente cliente = clienteRepository.findById(idCliente)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe el cliente solicitado"
            ));
        if (cliente.getEstado() != EstadoCatalogo.ACTIVO) {
            throw new OperacionNoPermitidaException(
                "No se puede cotizar a un cliente inactivo"
            );
        }
        return cliente;
    }

    private Producto buscarProductoActivo(Long idProducto) {
        Producto producto = productoRepository.findById(idProducto)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe el producto solicitado: " + idProducto
            ));
        if (producto.getEstado() != EstadoCatalogo.ACTIVO) {
            throw new OperacionNoPermitidaException(
                "No se puede cotizar un producto inactivo: "
                    + producto.getCodigoInterno()
            );
        }
        return producto;
    }

    private UnidadMedida buscarUnidadActiva(Long idUnidad) {
        UnidadMedida unidad = unidadMedidaRepository.findById(idUnidad)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la unidad de medida solicitada: " + idUnidad
            ));
        if (unidad.getEstado() != EstadoCatalogo.ACTIVO) {
            throw new OperacionNoPermitidaException(
                "No se puede cotizar con una unidad de medida inactiva"
            );
        }
        return unidad;
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

    private Cotizacion buscarCotizacion(Long id) {
        return cotizacionRepository.findDetalleById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la cotización solicitada"
            ));
    }

    private void validarEditable(Cotizacion cotizacion) {
        if (cotizacion.getEstado() != EstadoCotizacion.PENDIENTE) {
            throw new OperacionNoPermitidaException(
                "Solo se puede editar una cotización PENDIENTE y vigente"
            );
        }
    }

    private BigDecimal normalizarCantidad(
        BigDecimal cantidad,
        UnidadMedida unidad
    ) {
        BigDecimal normalizada;
        try {
            normalizada = cantidad.setScale(ESCALA_CANTIDAD, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new SolicitudInvalidaException(
                "La cantidad admite como máximo 3 decimales"
            );
        }
        if (!unidad.isPermiteDecimales()
            && normalizada.stripTrailingZeros().scale() > 0) {
            throw new SolicitudInvalidaException(
                "La unidad " + unidad.getCodigo() + " no admite cantidades decimales"
            );
        }
        return normalizada;
    }

    private BigDecimal validarDinero(BigDecimal valor, String campo) {
        BigDecimal normalizado = dinero(valor);
        int enteros = Math.max(0, normalizado.precision() - normalizado.scale());
        if (enteros > MAX_ENTEROS_DINERO) {
            throw new SolicitudInvalidaException(
                campo + " supera el importe máximo permitido"
            );
        }
        return normalizado;
    }

    private BigDecimal dinero(BigDecimal valor) {
        return valor.setScale(ESCALA_DINERO, RoundingMode.HALF_UP);
    }

    private void validarVencimiento(LocalDate fechaVencimiento) {
        if (fechaVencimiento != null && fechaVencimiento.isBefore(hoy())) {
            throw new SolicitudInvalidaException(
                "La fecha de vencimiento no puede estar vencida"
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

    private String normalizarTipoPrecio(String tipoPrecio) {
        return tipoPrecio == null || tipoPrecio.isBlank()
            ? "MINORISTA"
            : tipoPrecio.strip().toUpperCase(Locale.ROOT);
    }

    private void actualizarVencidas() {
        cotizacionRepository.marcarVencidas(
            hoy(),
            EstadoCotizacion.VENCIDA,
            ESTADOS_VIGENTES
        );
    }

    private LocalDate hoy() {
        return LocalDate.now(ZONA_NEGOCIO);
    }
}
