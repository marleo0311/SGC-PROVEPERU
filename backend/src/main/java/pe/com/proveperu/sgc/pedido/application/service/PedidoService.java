package pe.com.proveperu.sgc.pedido.application.service;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
import pe.com.proveperu.sgc.cotizacion.domain.model.Cotizacion;
import pe.com.proveperu.sgc.cotizacion.domain.model.DetalleCotizacion;
import pe.com.proveperu.sgc.cotizacion.domain.model.EstadoCotizacion;
import pe.com.proveperu.sgc.cotizacion.infrastructure.persistence.CotizacionRepository;
import pe.com.proveperu.sgc.inventario.application.service.InventarioService;
import pe.com.proveperu.sgc.inventario.domain.model.Sede;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.SedeRepository;
import pe.com.proveperu.sgc.pedido.api.dto.CotizacionConvertirPedidoRequest;
import pe.com.proveperu.sgc.pedido.api.dto.PedidoDetalleRequest;
import pe.com.proveperu.sgc.pedido.api.dto.PedidoDetalleResponse;
import pe.com.proveperu.sgc.pedido.api.dto.PedidoGuardarRequest;
import pe.com.proveperu.sgc.pedido.api.dto.PedidoResponse;
import pe.com.proveperu.sgc.pedido.api.dto.PedidoResumenResponse;
import pe.com.proveperu.sgc.pedido.api.dto.ReservaStockResponse;
import pe.com.proveperu.sgc.pedido.domain.model.CanalPedido;
import pe.com.proveperu.sgc.pedido.domain.model.DetallePedido;
import pe.com.proveperu.sgc.pedido.domain.model.EstadoPedido;
import pe.com.proveperu.sgc.pedido.domain.model.EstadoReservaStock;
import pe.com.proveperu.sgc.pedido.domain.model.Pedido;
import pe.com.proveperu.sgc.pedido.domain.model.ReservaStock;
import pe.com.proveperu.sgc.pedido.infrastructure.persistence.PedidoRepository;
import pe.com.proveperu.sgc.pedido.infrastructure.persistence.ReservaStockRepository;
import pe.com.proveperu.sgc.security.application.exception.OperacionNoPermitidaException;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;
import pe.com.proveperu.sgc.security.domain.model.EstadoUsuario;
import pe.com.proveperu.sgc.security.domain.model.Usuario;
import pe.com.proveperu.sgc.security.infrastructure.persistence.UsuarioRepository;
import pe.com.proveperu.sgc.shared.api.dto.PaginaResponse;
import pe.com.proveperu.sgc.shared.application.exception.SolicitudInvalidaException;

@Service
@RequiredArgsConstructor
public class PedidoService {

    private static final int ESCALA_DINERO = 2;
    private static final int ESCALA_CANTIDAD = 3;
    private static final int MAX_ENTEROS_DINERO = 12;
    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Lima");
    private static final List<EstadoCotizacion> ESTADOS_COTIZACION_VIGENTES = List.of(
        EstadoCotizacion.PENDIENTE,
        EstadoCotizacion.ACEPTADA
    );

    private final PedidoRepository pedidoRepository;
    private final ReservaStockRepository reservaRepository;
    private final CotizacionRepository cotizacionRepository;
    private final ClienteRepository clienteRepository;
    private final ClientePrecioEspecialRepository precioEspecialRepository;
    private final ProductoRepository productoRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;
    private final ProductoUnidadConversionRepository conversionRepository;
    private final PrecioProductoRepository precioRepository;
    private final UsuarioRepository usuarioRepository;
    private final SedeRepository sedeRepository;
    private final InventarioService inventarioService;

    @Transactional
    public PaginaResponse<PedidoResumenResponse> listar(
        Long idCliente,
        CanalPedido canal,
        EstadoPedido estado,
        LocalDate desde,
        LocalDate hasta,
        Pageable pageable
    ) {
        validarRango(desde, hasta);
        Page<PedidoResumenResponse> pagina = pedidoRepository.findAll(
            crearFiltros(idCliente, canal, estado, desde, hasta),
            pageable
        ).map(PedidoResumenResponse::from);
        return PaginaResponse.from(pagina);
    }

    @Transactional(readOnly = true)
    public PedidoResponse obtener(Long id) {
        return respuesta(buscarPedido(id));
    }

    @Transactional
    public PedidoResponse crear(
        PedidoGuardarRequest request,
        String usuarioLogin,
        boolean puedeAplicarDescuento
    ) {
        Cliente cliente = buscarClienteActivo(request.idCliente());
        Usuario usuario = buscarUsuarioActivo(usuarioLogin);
        Sede sede = resolverSede(request.idSede());

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setUsuario(usuario);
        pedido.setSede(sede);
        pedido.setCanal(request.canal());
        pedido.setEstado(EstadoPedido.RECIBIDO);
        pedido.setObservacion(normalizarTexto(request.observacion()));
        aplicarDatosDirectos(pedido, request, puedeAplicarDescuento);
        return respuesta(pedidoRepository.saveAndFlush(pedido));
    }

    @Transactional
    public PedidoResponse convertirCotizacion(
        Long idCotizacion,
        CotizacionConvertirPedidoRequest request,
        String usuarioLogin
    ) {
        cotizacionRepository.marcarVencidas(
            hoy(),
            EstadoCotizacion.VENCIDA,
            ESTADOS_COTIZACION_VIGENTES
        );
        Cotizacion cotizacion = cotizacionRepository.findForUpdate(idCotizacion)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la cotización solicitada"
            ));
        if (cotizacion.getEstado() != EstadoCotizacion.ACEPTADA) {
            throw new OperacionNoPermitidaException(
                "Solo una cotización ACEPTADA y vigente puede convertirse en pedido"
            );
        }
        if (pedidoRepository.existsByCotizacionId(idCotizacion)) {
            throw new OperacionNoPermitidaException(
                "La cotización ya fue convertida en un pedido"
            );
        }

        Usuario usuario = buscarUsuarioActivo(usuarioLogin);
        Sede sede = resolverSede(request.idSede());
        Pedido pedido = new Pedido();
        pedido.setCliente(cotizacion.getCliente());
        pedido.setCotizacion(cotizacion);
        pedido.setUsuario(usuario);
        pedido.setSede(sede);
        pedido.setCanal(request.canal() == null
            ? CanalPedido.PRESENCIAL
            : request.canal());
        pedido.setEstado(EstadoPedido.COTIZADO);
        pedido.setObservacion(normalizarTexto(request.observacion()));
        pedido.setSubtotal(cotizacion.getSubtotal());
        pedido.setIgv(cotizacion.getIgv());
        pedido.setTotal(cotizacion.getTotal());

        for (DetalleCotizacion origen : cotizacion.getDetalles()) {
            DetallePedido detalle = new DetallePedido();
            detalle.setProducto(origen.getProducto());
            detalle.setUnidadMedida(origen.getUnidadMedida());
            detalle.setCantidad(origen.getCantidad());
            detalle.setCantidadBase(cantidadBase(
                origen.getProducto(),
                origen.getUnidadMedida(),
                origen.getCantidad()
            ));
            detalle.setPrecioUnitario(origen.getPrecioUnitario());
            detalle.setDescuento(origen.getDescuento());
            detalle.setSubtotal(origen.getSubtotal());
            pedido.agregarDetalle(detalle);
        }

        pedido = pedidoRepository.saveAndFlush(pedido);
        cotizacion.setEstado(EstadoCotizacion.CONVERTIDA);
        cotizacionRepository.saveAndFlush(cotizacion);
        return respuesta(pedido);
    }

    @Transactional
    public PedidoResponse confirmar(Long id, String usuarioLogin) {
        Pedido pedido = buscarPedidoParaActualizar(id);
        if (estadoConReserva(pedido.getEstado())) {
            return respuesta(pedido);
        }
        if (pedido.getEstado() != EstadoPedido.RECIBIDO
            && pedido.getEstado() != EstadoPedido.COTIZADO) {
            throw new OperacionNoPermitidaException(
                "El pedido " + pedido.getEstado() + " no puede confirmarse"
            );
        }
        if (!reservaRepository.findAllByPedidoIdOrderByIdAsc(id).isEmpty()) {
            throw new OperacionNoPermitidaException(
                "El pedido ya tiene reservas registradas"
            );
        }

        Usuario usuario = buscarUsuarioActivo(usuarioLogin);
        List<DetallePedido> detalles = pedido.getDetalles().stream()
            .sorted(Comparator.comparing(detalle -> detalle.getProducto().getId()))
            .toList();
        List<ReservaStock> reservas = new ArrayList<>();
        for (DetallePedido detalle : detalles) {
            inventarioService.reservarParaPedido(
                pedido.getSede(),
                detalle.getProducto(),
                detalle.getUnidadMedida(),
                detalle.getCantidad(),
                detalle.getCantidadBase(),
                usuario,
                pedido.getId()
            );

            ReservaStock reserva = new ReservaStock();
            reserva.setPedido(pedido);
            reserva.setDetallePedido(detalle);
            reserva.setSede(pedido.getSede());
            reserva.setProducto(detalle.getProducto());
            reserva.setCantidad(detalle.getCantidadBase());
            reserva.setEstado(EstadoReservaStock.ACTIVA);
            reservas.add(reserva);
        }
        reservaRepository.saveAll(reservas);
        reservaRepository.flush();
        pedido.setEstado(EstadoPedido.CONFIRMADO);
        return respuesta(pedidoRepository.saveAndFlush(pedido));
    }

    @Transactional
    public PedidoResponse cancelar(Long id, String usuarioLogin) {
        Pedido pedido = buscarPedidoParaActualizar(id);
        if (pedido.getEstado() == EstadoPedido.CANCELADO) {
            return respuesta(pedido);
        }
        if (pedido.getEstado() == EstadoPedido.ENTREGADO) {
            throw new OperacionNoPermitidaException(
                "Un pedido ENTREGADO no puede cancelarse"
            );
        }

        Usuario usuario = buscarUsuarioActivo(usuarioLogin);
        List<ReservaStock> activas = reservaRepository
            .findAllByPedidoIdAndEstadoOrderByProductoIdAsc(
                id,
                EstadoReservaStock.ACTIVA
            );
        Instant ahora = Instant.now();
        for (ReservaStock reserva : activas) {
            DetallePedido detalle = reserva.getDetallePedido();
            inventarioService.liberarReservaDePedido(
                reserva.getSede(),
                reserva.getProducto(),
                detalle.getUnidadMedida(),
                detalle.getCantidad(),
                reserva.getCantidad(),
                usuario,
                pedido.getId()
            );
            reserva.setEstado(EstadoReservaStock.LIBERADA);
            reserva.setFechaLiberacion(ahora);
        }
        reservaRepository.saveAll(activas);
        pedido.setEstado(EstadoPedido.CANCELADO);
        return respuesta(pedidoRepository.saveAndFlush(pedido));
    }

    @Transactional
    public PedidoResponse cambiarEstado(Long id, EstadoPedido nuevoEstado) {
        if (nuevoEstado == EstadoPedido.CONFIRMADO
            || nuevoEstado == EstadoPedido.CANCELADO) {
            throw new OperacionNoPermitidaException(
                "Use el endpoint específico para confirmar o cancelar el pedido"
            );
        }
        Pedido pedido = buscarPedidoParaActualizar(id);
        if (pedido.getEstado() == nuevoEstado) {
            return respuesta(pedido);
        }
        if (!transicionPermitida(pedido.getEstado(), nuevoEstado)) {
            throw new OperacionNoPermitidaException(
                "No se permite cambiar el pedido de " + pedido.getEstado()
                    + " a " + nuevoEstado
            );
        }
        pedido.setEstado(nuevoEstado);
        return respuesta(pedidoRepository.saveAndFlush(pedido));
    }

    @Transactional(readOnly = true)
    public List<ReservaStockResponse> listarReservas(Long idPedido) {
        if (!pedidoRepository.existsById(idPedido)) {
            throw new RecursoNoEncontradoException("No existe el pedido solicitado");
        }
        return reservaRepository.findAllByPedidoIdOrderByIdAsc(idPedido)
            .stream()
            .map(ReservaStockResponse::from)
            .toList();
    }

    private void aplicarDatosDirectos(
        Pedido pedido,
        PedidoGuardarRequest request,
        boolean puedeAplicarDescuento
    ) {
        pedido.setIgv(validarDinero(request.igv(), "El IGV"));
        Set<Long> productosUnicos = new HashSet<>();
        BigDecimal subtotal = dinero(BigDecimal.ZERO);
        for (PedidoDetalleRequest item : request.detalles()) {
            if (!productosUnicos.add(item.idProducto())) {
                throw new SolicitudInvalidaException(
                    "No se puede repetir un producto en el pedido"
                );
            }
            DetallePedido detalle = crearDetalleDirecto(
                pedido.getCliente(),
                item,
                puedeAplicarDescuento
            );
            pedido.agregarDetalle(detalle);
            subtotal = subtotal.add(detalle.getSubtotal());
        }
        subtotal = validarDinero(subtotal, "El subtotal");
        pedido.setSubtotal(subtotal);
        pedido.setTotal(validarDinero(
            subtotal.add(pedido.getIgv()),
            "El total"
        ));
    }

    private DetallePedido crearDetalleDirecto(
        Cliente cliente,
        PedidoDetalleRequest item,
        boolean puedeAplicarDescuento
    ) {
        Producto producto = buscarProductoActivo(item.idProducto());
        UnidadMedida unidad = buscarUnidadActiva(item.idUnidadMedida());
        BigDecimal cantidad = normalizarCantidad(item.cantidad(), unidad);
        BigDecimal factor = factorAUnidadBase(producto, unidad);
        BigDecimal precioBase = resolverPrecioBase(
            cliente,
            producto,
            normalizarTipoPrecio(item.tipoPrecio()),
            hoy()
        );
        BigDecimal precioUnitario = validarDinero(
            precioBase.multiply(factor),
            "El precio unitario"
        );
        BigDecimal descuento = validarDinero(item.descuento(), "El descuento");
        if (descuento.compareTo(BigDecimal.ZERO) > 0 && !puedeAplicarDescuento) {
            throw new OperacionNoPermitidaException(
                "No tiene permiso para aplicar descuentos en pedidos"
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

        DetallePedido detalle = new DetallePedido();
        detalle.setProducto(producto);
        detalle.setUnidadMedida(unidad);
        detalle.setCantidad(cantidad);
        detalle.setCantidadBase(cantidad.multiply(factor).setScale(
            ESCALA_CANTIDAD,
            RoundingMode.HALF_UP
        ));
        if (detalle.getCantidadBase().compareTo(BigDecimal.ZERO) <= 0) {
            throw new SolicitudInvalidaException(
                "La conversión produce una cantidad menor a la precisión admitida"
            );
        }
        detalle.setPrecioUnitario(precioUnitario);
        detalle.setDescuento(descuento);
        detalle.setSubtotal(importeBruto.subtract(descuento));
        return detalle;
    }

    private PedidoResponse respuesta(Pedido pedido) {
        List<ReservaStockResponse> reservas = reservaRepository
            .findAllByPedidoIdOrderByIdAsc(pedido.getId())
            .stream()
            .map(ReservaStockResponse::from)
            .toList();
        return new PedidoResponse(
            PedidoResumenResponse.from(pedido),
            pedido.getDetalles().stream()
                .map(PedidoDetalleResponse::from)
                .toList(),
            reservas,
            pedido.getFechaActualizacion()
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

    private BigDecimal cantidadBase(
        Producto producto,
        UnidadMedida unidad,
        BigDecimal cantidad
    ) {
        BigDecimal resultado = cantidad.multiply(
            factorAUnidadBase(producto, unidad)
        ).setScale(ESCALA_CANTIDAD, RoundingMode.HALF_UP);
        if (resultado.compareTo(BigDecimal.ZERO) <= 0) {
            throw new SolicitudInvalidaException(
                "La conversión produce una cantidad menor a la precisión admitida"
            );
        }
        return resultado;
    }

    private Specification<Pedido> crearFiltros(
        Long idCliente,
        CanalPedido canal,
        EstadoPedido estado,
        LocalDate desde,
        LocalDate hasta
    ) {
        Instant instanteDesde = desde == null
            ? null
            : desde.atStartOfDay(ZONA_NEGOCIO).toInstant();
        Instant instanteHasta = hasta == null
            ? null
            : hasta.plusDays(1).atStartOfDay(ZONA_NEGOCIO).toInstant();
        return (root, query, builder) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (idCliente != null) {
                condiciones.add(builder.equal(root.get("cliente").get("id"), idCliente));
            }
            if (canal != null) {
                condiciones.add(builder.equal(root.get("canal"), canal));
            }
            if (estado != null) {
                condiciones.add(builder.equal(root.get("estado"), estado));
            }
            if (instanteDesde != null) {
                condiciones.add(builder.greaterThanOrEqualTo(
                    root.get("fechaHora"),
                    instanteDesde
                ));
            }
            if (instanteHasta != null) {
                condiciones.add(builder.lessThan(root.get("fechaHora"), instanteHasta));
            }
            return builder.and(condiciones.toArray(Predicate[]::new));
        };
    }

    private Pedido buscarPedido(Long id) {
        return pedidoRepository.findDetalleById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe el pedido solicitado"
            ));
    }

    private Pedido buscarPedidoParaActualizar(Long id) {
        return pedidoRepository.findForUpdate(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe el pedido solicitado"
            ));
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
                "No se puede registrar un pedido para un cliente inactivo"
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
                "No se puede pedir un producto inactivo: "
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
                "No se puede usar una unidad de medida inactiva"
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

    private Sede resolverSede(Long idSede) {
        Sede sede = idSede == null
            ? sedeRepository.findFirstByEstadoIgnoreCaseOrderByIdAsc("ACTIVO")
                .orElseThrow(() -> new RecursoNoEncontradoException(
                    "No existe una sede activa"
                ))
            : sedeRepository.findById(idSede)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                    "No existe la sede solicitada"
                ));
        if (!"ACTIVO".equalsIgnoreCase(sede.getEstado())) {
            throw new OperacionNoPermitidaException(
                "No se puede operar el pedido en una sede inactiva"
            );
        }
        return sede;
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

    private boolean estadoConReserva(EstadoPedido estado) {
        return estado == EstadoPedido.CONFIRMADO
            || estado == EstadoPedido.PAGADO
            || estado == EstadoPedido.EN_PREPARACION
            || estado == EstadoPedido.LISTO
            || estado == EstadoPedido.ENTREGADO;
    }

    private boolean transicionPermitida(EstadoPedido actual, EstadoPedido nuevo) {
        return switch (actual) {
            case RECIBIDO -> nuevo == EstadoPedido.COTIZADO;
            case CONFIRMADO -> nuevo == EstadoPedido.PAGADO
                || nuevo == EstadoPedido.EN_PREPARACION;
            case PAGADO -> nuevo == EstadoPedido.EN_PREPARACION;
            case EN_PREPARACION -> nuevo == EstadoPedido.LISTO;
            case LISTO -> nuevo == EstadoPedido.ENTREGADO;
            case COTIZADO, ENTREGADO, CANCELADO -> false;
        };
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

    private String normalizarTexto(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }

    private LocalDate hoy() {
        return LocalDate.now(ZONA_NEGOCIO);
    }
}
