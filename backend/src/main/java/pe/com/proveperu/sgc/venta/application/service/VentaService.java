package pe.com.proveperu.sgc.venta.application.service;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
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
import pe.com.proveperu.sgc.caja.application.service.CajaService;
import pe.com.proveperu.sgc.cliente.domain.model.Cliente;
import pe.com.proveperu.sgc.cliente.domain.model.ClientePrecioEspecial;
import pe.com.proveperu.sgc.cliente.infrastructure.persistence.ClientePrecioEspecialRepository;
import pe.com.proveperu.sgc.cliente.infrastructure.persistence.ClienteRepository;
import pe.com.proveperu.sgc.comprobante.application.service.ComprobanteService;
import pe.com.proveperu.sgc.configuracion.domain.model.MetodoPago;
import pe.com.proveperu.sgc.configuracion.infrastructure.persistence.MetodoPagoRepository;
import pe.com.proveperu.sgc.inventario.application.service.InventarioService;
import pe.com.proveperu.sgc.inventario.domain.model.Sede;
import pe.com.proveperu.sgc.inventario.domain.model.ExistenciaPresentacion;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.SedeRepository;
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
import pe.com.proveperu.sgc.shared.application.service.CalculoTributario;
import pe.com.proveperu.sgc.venta.api.dto.CuentaCobrarVentaResponse;
import pe.com.proveperu.sgc.venta.api.dto.MetodoPagoVentaResponse;
import pe.com.proveperu.sgc.venta.api.dto.PagoClienteResponse;
import pe.com.proveperu.sgc.venta.api.dto.VentaAnularRequest;
import pe.com.proveperu.sgc.venta.api.dto.VentaCrearRequest;
import pe.com.proveperu.sgc.venta.api.dto.VentaDetalleResponse;
import pe.com.proveperu.sgc.venta.api.dto.VentaItemRequest;
import pe.com.proveperu.sgc.venta.api.dto.VentaResponse;
import pe.com.proveperu.sgc.venta.api.dto.VentaResumenResponse;
import pe.com.proveperu.sgc.venta.domain.model.CondicionPagoVenta;
import pe.com.proveperu.sgc.venta.domain.model.CuentaCobrar;
import pe.com.proveperu.sgc.venta.domain.model.DetalleVenta;
import pe.com.proveperu.sgc.venta.domain.model.EstadoCuentaCobrar;
import pe.com.proveperu.sgc.venta.domain.model.EstadoVenta;
import pe.com.proveperu.sgc.venta.domain.model.PagoCliente;
import pe.com.proveperu.sgc.venta.domain.model.OrigenCuentaCobrar;
import pe.com.proveperu.sgc.venta.domain.model.Venta;
import pe.com.proveperu.sgc.venta.infrastructure.persistence.CuentaCobrarRepository;
import pe.com.proveperu.sgc.venta.infrastructure.persistence.PagoClienteRepository;
import pe.com.proveperu.sgc.venta.infrastructure.persistence.VentaRepository;

@Service
@RequiredArgsConstructor
public class VentaService {

    private static final int ESCALA_DINERO = 2;
    private static final int ESCALA_CANTIDAD = 3;
    private static final int MAX_ENTEROS_DINERO = 12;
    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Lima");
    private static final Set<EstadoPedido> ESTADOS_PEDIDO_VENDIBLES = EnumSet.of(
        EstadoPedido.CONFIRMADO,
        EstadoPedido.PAGADO,
        EstadoPedido.EN_PREPARACION,
        EstadoPedido.LISTO,
        EstadoPedido.ENTREGADO
    );

    private final VentaRepository ventaRepository;
    private final CuentaCobrarRepository cuentaRepository;
    private final PagoClienteRepository pagoRepository;
    private final PedidoRepository pedidoRepository;
    private final ReservaStockRepository reservaRepository;
    private final ClienteRepository clienteRepository;
    private final ClientePrecioEspecialRepository precioEspecialRepository;
    private final ProductoRepository productoRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;
    private final ProductoUnidadConversionRepository conversionRepository;
    private final PrecioProductoRepository precioRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final SedeRepository sedeRepository;
    private final InventarioService inventarioService;
    private final CajaService cajaService;
    private final ComprobanteService comprobanteService;

    @Transactional
    public PaginaResponse<VentaResumenResponse> listar(
        Long idCliente,
        EstadoVenta estado,
        CondicionPagoVenta condicionPago,
        LocalDate desde,
        LocalDate hasta,
        Pageable pageable
    ) {
        validarRango(desde, hasta);
        Page<VentaResumenResponse> pagina = ventaRepository.findAll(
            crearFiltros(idCliente, estado, condicionPago, desde, hasta),
            pageable
        ).map(VentaResumenResponse::from);
        return PaginaResponse.from(pagina);
    }

    @Transactional(readOnly = true)
    public VentaResponse obtener(Long id) {
        return respuesta(buscarVenta(id));
    }

    @Transactional
    public VentaResponse crear(
        VentaCrearRequest request,
        String usuarioLogin,
        boolean puedeAplicarDescuento
    ) {
        Usuario vendedor = buscarUsuarioActivo(usuarioLogin);
        Venta venta = new Venta();
        venta.setVendedor(vendedor);
        venta.setTipoVenta(request.tipoVenta());
        venta.setCondicionPago(request.condicionPago());
        venta.setTipoComprobante(request.tipoComprobante());
        venta.setEstado(EstadoVenta.REGISTRADA);

        Pedido pedido = request.idPedido() == null
            ? null
            : prepararDesdePedido(venta, request);
        if (pedido == null) {
            prepararVentaDirecta(venta, request, puedeAplicarDescuento);
        }
        comprobanteService.validarEmision(venta);
        validarCondicionPago(request, venta);
        venta = ventaRepository.saveAndFlush(venta);

        if (pedido == null) {
            descontarVentaDirecta(venta, vendedor);
        } else {
            consumirReservas(venta, pedido, vendedor);
        }
        registrarFinanciamientoYPago(venta, request, vendedor);
        comprobanteService.emitirParaVenta(venta);
        return respuesta(buscarVenta(venta.getId()));
    }

    @Transactional
    public VentaResponse anular(
        Long id,
        VentaAnularRequest request,
        String usuarioLogin
    ) {
        Venta venta = ventaRepository.findForUpdate(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la venta solicitada"
            ));
        if (venta.getEstado() == EstadoVenta.ANULADA) {
            return respuesta(buscarVenta(id));
        }
        if (venta.getEstado() != EstadoVenta.REGISTRADA) {
            throw new OperacionNoPermitidaException(
                "La venta " + venta.getEstado() + " no puede anularse"
            );
        }
        Usuario usuario = buscarUsuarioActivo(usuarioLogin);
        inventarioService.restaurarPresentacionesVenta(venta.getId());
        List<DetalleVenta> detalles = venta.getDetalles().stream()
            .sorted(Comparator.comparing(detalle -> detalle.getProducto().getId()))
            .toList();
        for (DetalleVenta detalle : detalles) {
            inventarioService.restaurarVentaAnulada(
                venta.getAlmacenSalida(),
                detalle.getProducto(),
                detalle.getUnidadMedida(),
                detalle.getCantidad(),
                detalle.getCantidadBase(),
                usuario,
                venta.getId()
            );
        }

        cuentaRepository.findByVentaId(id).ifPresent(cuenta -> {
            cuenta.setSaldoPendiente(BigDecimal.ZERO.setScale(ESCALA_DINERO));
            cuenta.setEstado(EstadoCuentaCobrar.ANULADO);
            cuentaRepository.save(cuenta);
        });
        venta.setEstado(EstadoVenta.ANULADA);
        venta.setFechaAnulacion(Instant.now());
        venta.setMotivoAnulacion(request.motivo().strip());
        ventaRepository.saveAndFlush(venta);
        comprobanteService.anularPorVenta(venta, usuario, request.motivo());
        return respuesta(buscarVenta(id));
    }

    @Transactional(readOnly = true)
    public List<MetodoPagoVentaResponse> listarMetodosPago() {
        return metodoPagoRepository.findAllByEstadoIgnoreCaseOrderByNombreAsc("ACTIVO")
            .stream()
            .map(MetodoPagoVentaResponse::from)
            .toList();
    }

    private Pedido prepararDesdePedido(Venta venta, VentaCrearRequest request) {
        Pedido pedido = pedidoRepository.findForUpdate(request.idPedido())
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe el pedido solicitado"
            ));
        if (!ESTADOS_PEDIDO_VENDIBLES.contains(pedido.getEstado())) {
            throw new OperacionNoPermitidaException(
                "El pedido debe estar CONFIRMADO y mantener sus reservas activas"
            );
        }
        if (ventaRepository.existsByPedidoId(pedido.getId())) {
            throw new OperacionNoPermitidaException(
                "El pedido ya fue convertido en una venta"
            );
        }
        if (request.items() != null && !request.items().isEmpty()) {
            throw new SolicitudInvalidaException(
                "Una venta desde pedido toma sus productos del pedido; no envíe items"
            );
        }
        if (request.idCliente() != null && (pedido.getCliente() == null
            || !request.idCliente().equals(pedido.getCliente().getId()))) {
            throw new SolicitudInvalidaException(
                "El cliente enviado no corresponde al cliente del pedido"
            );
        }
        if (request.idSede() != null
            && !request.idSede().equals(pedido.getSede().getId())) {
            throw new SolicitudInvalidaException(
                "La sede enviada no corresponde a la sede del pedido"
            );
        }
        if (request.aplicarIgv() != null
            && request.aplicarIgv() != (pedido.getIgv().compareTo(BigDecimal.ZERO) > 0)) {
            throw new SolicitudInvalidaException(
                "El IGV de una venta desde pedido no puede modificarse"
            );
        }

        venta.setPedido(pedido);
        venta.setCliente(pedido.getCliente());
        venta.setSede(resolverSedeFacturacion());
        venta.setAlmacenSalida(pedido.getSede());
        BigDecimal descuentoTotal = dinero(BigDecimal.ZERO);
        for (DetallePedido origen : pedido.getDetalles()) {
            DetalleVenta detalle = new DetalleVenta();
            detalle.setProducto(origen.getProducto());
            detalle.setUnidadMedida(origen.getUnidadMedida());
            detalle.setCantidad(origen.getCantidad());
            detalle.setCantidadBase(origen.getCantidadBase());
            detalle.setPrecioUnitario(origen.getPrecioUnitario());
            detalle.setDescuento(origen.getDescuento());
            detalle.setSubtotal(origen.getSubtotal());
            venta.agregarDetalle(detalle);
            descuentoTotal = descuentoTotal.add(origen.getDescuento());
        }
        venta.setSubtotal(pedido.getSubtotal());
        venta.setIgv(pedido.getIgv());
        venta.setDescuentoTotal(validarDinero(
            descuentoTotal,
            "El descuento total"
        ));
        venta.setTotal(pedido.getTotal());
        return pedido;
    }

    private void prepararVentaDirecta(
        Venta venta,
        VentaCrearRequest request,
        boolean puedeAplicarDescuento
    ) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new SolicitudInvalidaException(
                "Una venta directa debe contener al menos un producto"
            );
        }
        Cliente cliente = buscarClienteActivo(request.idCliente());
        venta.setCliente(cliente);
        venta.setSede(resolverSedeFacturacion());
        venta.setAlmacenSalida(resolverSede(request.idSede()));

        BigDecimal importeFinal = dinero(BigDecimal.ZERO);
        BigDecimal descuentoTotal = dinero(BigDecimal.ZERO);
        for (VentaItemRequest item : request.items()) {
            DetalleVenta detalle = crearDetalleDirecto(
                cliente,
                venta.getTipoVenta().name(),
                item,
                puedeAplicarDescuento,
                venta.getAlmacenSalida()
            );
            venta.agregarDetalle(detalle);
            importeFinal = importeFinal.add(detalle.getSubtotal());
            descuentoTotal = descuentoTotal.add(detalle.getDescuento());
        }
        importeFinal = validarDinero(importeFinal, "El total");
        boolean aplicarIgv = request.aplicarIgv() == null || request.aplicarIgv();
        CalculoTributario.Totales totales = CalculoTributario.desdePrecioFinal(
            importeFinal,
            aplicarIgv
        );
        venta.setSubtotal(totales.subtotal());
        venta.setIgv(totales.igv());
        venta.setDescuentoTotal(validarDinero(
            descuentoTotal,
            "El descuento total"
        ));
        venta.setTotal(totales.total());
        if (venta.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new SolicitudInvalidaException(
                "El total de la venta debe ser mayor que cero"
            );
        }
    }

    private DetalleVenta crearDetalleDirecto(
        Cliente cliente,
        String tipoPrecio,
        VentaItemRequest item,
        boolean puedeAplicarDescuento,
        Sede sede
    ) {
        Producto producto = buscarProductoActivo(item.idProducto());
        ExistenciaPresentacion existencia = item.idExistenciaPresentacion() == null
            ? null
            : inventarioService.buscarPresentacionParaVenta(
                item.idExistenciaPresentacion(), sede, producto
            );
        UnidadMedida unidad = existencia == null
            ? buscarUnidadActiva(item.idUnidadMedida())
            : existencia.getPresentacion().getUnidadMedida();
        if (existencia != null
            && (item.idUnidadMedida() == null
                || !unidad.getId().equals(item.idUnidadMedida()))) {
            throw new SolicitudInvalidaException(
                "La unidad enviada no corresponde a la presentación física seleccionada"
            );
        }
        BigDecimal cantidad = normalizarCantidad(item.cantidad(), unidad);
        if (existencia != null && cantidad.compareTo(BigDecimal.ONE.setScale(3)) != 0) {
            throw new SolicitudInvalidaException(
                "Cada caja, paquete o rollo físico se vende como una línea de cantidad 1"
            );
        }
        BigDecimal factor = existencia == null
            ? factorAUnidadBase(producto, unidad)
            : existencia.getCantidadDisponibleBase();
        BigDecimal precioUnitario = validarDinero(
            resolverPrecioBase(cliente, producto, tipoPrecio, hoy())
                .multiply(factor)
                .setScale(ESCALA_DINERO, RoundingMode.HALF_UP),
            "El precio unitario"
        );
        if (item.precioUnitario() != null
            && validarDinero(item.precioUnitario(), "El precio esperado")
                .compareTo(precioUnitario) != 0) {
            throw new OperacionNoPermitidaException(
                "El precio vigente del producto " + producto.getCodigoInterno()
                    + " es " + precioUnitario.toPlainString()
            );
        }
        BigDecimal descuento = validarDinero(item.descuento(), "El descuento");
        if (descuento.compareTo(BigDecimal.ZERO) > 0 && !puedeAplicarDescuento) {
            throw new OperacionNoPermitidaException(
                "No tiene permiso para aplicar descuentos en ventas"
            );
        }
        BigDecimal importeBruto = validarDinero(
            cantidad.multiply(precioUnitario).setScale(
                ESCALA_DINERO, RoundingMode.HALF_UP
            ),
            "El importe del detalle"
        );
        if (descuento.compareTo(importeBruto) > 0) {
            throw new SolicitudInvalidaException(
                "El descuento no puede superar el importe del producto "
                    + producto.getCodigoInterno()
            );
        }

        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(producto);
        detalle.setUnidadMedida(unidad);
        detalle.setExistenciaPresentacion(existencia);
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

    private void descontarVentaDirecta(Venta venta, Usuario vendedor) {
        venta.getDetalles().stream()
            .sorted(Comparator.comparing(detalle -> detalle.getProducto().getId()))
            .forEach(detalle -> {
                inventarioService.consumirPresentacionesVenta(
                    venta.getAlmacenSalida(), detalle
                );
                inventarioService.registrarVentaDirecta(
                    venta.getAlmacenSalida(),
                    detalle.getProducto(),
                    detalle.getUnidadMedida(),
                    detalle.getCantidad(),
                    detalle.getCantidadBase(),
                    vendedor,
                    venta.getId()
                );
            });
    }

    private void consumirReservas(
        Venta venta,
        Pedido pedido,
        Usuario vendedor
    ) {
        List<ReservaStock> reservas = reservaRepository
            .findAllByPedidoIdAndEstadoOrderByProductoIdAsc(
                pedido.getId(),
                EstadoReservaStock.ACTIVA
            );
        if (reservas.size() != pedido.getDetalles().size()) {
            throw new OperacionNoPermitidaException(
                "El pedido no conserva todas sus reservas activas"
            );
        }
        Instant ahora = Instant.now();
        for (ReservaStock reserva : reservas) {
            DetallePedido detalle = reserva.getDetallePedido();
            if (!detalle.getProducto().getId().equals(reserva.getProducto().getId())
                || detalle.getCantidadBase().compareTo(reserva.getCantidad()) != 0) {
                throw new OperacionNoPermitidaException(
                    "La reserva del pedido no coincide con su detalle de producto"
                );
            }
            DetalleVenta detalleVenta = venta.getDetalles().stream()
                .filter(item -> item.getProducto().getId().equals(reserva.getProducto().getId()))
                .findFirst()
                .orElseThrow(() -> new OperacionNoPermitidaException(
                    "La venta no contiene el producto reservado"
                ));
            inventarioService.consumirPresentacionesVentaReservada(
                reserva.getSede(), detalleVenta
            );
            inventarioService.consumirReservaParaVenta(
                reserva.getSede(),
                reserva.getProducto(),
                detalle.getUnidadMedida(),
                detalle.getCantidad(),
                reserva.getCantidad(),
                vendedor,
                venta.getId(),
                pedido.getId()
            );
            reserva.setEstado(EstadoReservaStock.CONSUMIDA);
            reserva.setFechaLiberacion(ahora);
        }
        reservaRepository.saveAll(reservas);
        pedido.setEstado(EstadoPedido.ENTREGADO);
        pedidoRepository.save(pedido);
    }

    private void validarCondicionPago(VentaCrearRequest request, Venta venta) {
        if (venta.getCondicionPago() != CondicionPagoVenta.CONTADO
            && venta.getCliente() == null) {
            throw new SolicitudInvalidaException(
                "Una venta a crédito o parcial requiere un cliente identificado"
            );
        }
        if (venta.getCondicionPago() == CondicionPagoVenta.CREDITO) {
            if (request.idMetodoPago() != null || request.montoPagado() != null) {
                throw new SolicitudInvalidaException(
                    "Una venta a crédito no admite pago inicial ni método de pago"
                );
            }
            exigirVencimiento(request.fechaVencimiento());
        } else if (venta.getCondicionPago() == CondicionPagoVenta.PARCIAL) {
            if (request.idMetodoPago() == null || request.montoPagado() == null) {
                throw new SolicitudInvalidaException(
                    "Una venta parcial requiere método y monto de pago inicial"
                );
            }
            exigirVencimiento(request.fechaVencimiento());
            BigDecimal monto = validarDinero(request.montoPagado(), "El pago inicial");
            if (monto.compareTo(venta.getTotal()) >= 0) {
                throw new SolicitudInvalidaException(
                    "El pago inicial debe ser menor que el total en una venta parcial"
                );
            }
        } else {
            if (request.idMetodoPago() == null) {
                throw new SolicitudInvalidaException(
                    "Una venta al contado requiere un método de pago"
                );
            }
            if (request.montoPagado() != null
                && validarDinero(request.montoPagado(), "El monto pagado")
                    .compareTo(venta.getTotal()) != 0) {
                throw new SolicitudInvalidaException(
                    "El monto pagado debe coincidir con el total de la venta al contado"
                );
            }
        }
    }

    private void registrarFinanciamientoYPago(
        Venta venta,
        VentaCrearRequest request,
        Usuario usuario
    ) {
        BigDecimal importePagado = switch (venta.getCondicionPago()) {
            case CONTADO -> venta.getTotal();
            case CREDITO -> BigDecimal.ZERO.setScale(ESCALA_DINERO);
            case PARCIAL -> validarDinero(request.montoPagado(), "El pago inicial");
        };
        BigDecimal saldo = venta.getTotal().subtract(importePagado);
        CuentaCobrar cuenta = null;
        if (saldo.compareTo(BigDecimal.ZERO) > 0) {
            cuenta = new CuentaCobrar();
            cuenta.setVenta(venta);
            cuenta.setCliente(venta.getCliente());
            cuenta.setUsuarioCreacion(usuario);
            cuenta.setOrigen(OrigenCuentaCobrar.VENTA);
            cuenta.setFechaOrigen(venta.getFechaHora()
                .atZone(ZONA_NEGOCIO)
                .toLocalDate());
            cuenta.setTotal(venta.getTotal());
            cuenta.setImportePagado(importePagado);
            cuenta.setSaldoPendiente(saldo);
            cuenta.setFechaVencimiento(request.fechaVencimiento());
            cuenta.setEstado(importePagado.compareTo(BigDecimal.ZERO) > 0
                ? EstadoCuentaCobrar.PARCIAL
                : EstadoCuentaCobrar.PENDIENTE);
            cuenta = cuentaRepository.saveAndFlush(cuenta);
            venta.setCuentaCobrar(cuenta);
        }
        if (importePagado.compareTo(BigDecimal.ZERO) > 0) {
            MetodoPago metodo = buscarMetodoPagoActivo(request.idMetodoPago());
            PagoCliente pago = new PagoCliente();
            pago.setVenta(venta);
            pago.setCuentaCobrar(cuenta);
            pago.setMetodoPago(metodo);
            pago.setUsuario(usuario);
            pago.setMonto(importePagado);
            pago.setReferencia(normalizarTexto(request.referenciaPago()));
            pago = pagoRepository.saveAndFlush(pago);
            cajaService.registrarIngresoVenta(venta, pago, usuario);
        }
    }

    private VentaResponse respuesta(Venta venta) {
        List<PagoClienteResponse> pagos = pagoRepository
            .findAllByVentaIdOrderByFechaHoraDescIdDesc(venta.getId())
            .stream()
            .map(PagoClienteResponse::from)
            .toList();
        return new VentaResponse(
            VentaResumenResponse.from(venta),
            venta.getDetalles().stream()
                .map(VentaDetalleResponse::from)
                .toList(),
            CuentaCobrarVentaResponse.from(venta.getCuentaCobrar()),
            pagos
        );
    }

    private Specification<Venta> crearFiltros(
        Long idCliente,
        EstadoVenta estado,
        CondicionPagoVenta condicionPago,
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
            if (estado != null) {
                condiciones.add(builder.equal(root.get("estado"), estado));
            }
            if (condicionPago != null) {
                condiciones.add(builder.equal(
                    root.get("condicionPago"),
                    condicionPago
                ));
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

    private Venta buscarVenta(Long id) {
        return ventaRepository.findDetalleById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la venta solicitada"
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
                "No se puede vender a un cliente inactivo"
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
                "No se puede vender un producto inactivo: "
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
                "No se puede vender desde una sede inactiva"
            );
        }
        return sede;
    }

    private Sede resolverSedeFacturacion() {
        return sedeRepository
            .findFirstBySedeFacturacionTrueAndEstadoIgnoreCaseOrderByIdAsc("ACTIVO")
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe un local fiscal activo para emitir comprobantes"
            ));
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

    private BigDecimal normalizarCantidad(
        BigDecimal cantidadSolicitada,
        UnidadMedida unidad
    ) {
        BigDecimal cantidad;
        try {
            cantidad = cantidadSolicitada.setScale(
                ESCALA_CANTIDAD,
                RoundingMode.UNNECESSARY
            );
        } catch (ArithmeticException exception) {
            throw new SolicitudInvalidaException(
                "La cantidad admite como máximo tres decimales"
            );
        }
        if (!unidad.isPermiteDecimales()
            && cantidad.stripTrailingZeros().scale() > 0) {
            throw new SolicitudInvalidaException(
                "La unidad " + unidad.getCodigo() + " no permite cantidades decimales"
            );
        }
        return cantidad;
    }

    private BigDecimal validarDinero(BigDecimal valor, String campo) {
        try {
            BigDecimal normalizado = valor.setScale(
                ESCALA_DINERO,
                RoundingMode.UNNECESSARY
            );
            if (normalizado.precision() - normalizado.scale() > MAX_ENTEROS_DINERO) {
                throw new ArithmeticException();
            }
            return normalizado;
        } catch (ArithmeticException exception) {
            throw new SolicitudInvalidaException(
                campo + " admite hasta 12 enteros y 2 decimales"
            );
        }
    }

    private BigDecimal dinero(BigDecimal valor) {
        return valor.setScale(ESCALA_DINERO, RoundingMode.UNNECESSARY);
    }

    private void exigirVencimiento(LocalDate fechaVencimiento) {
        if (fechaVencimiento == null) {
            throw new SolicitudInvalidaException(
                "La fecha de vencimiento es obligatoria cuando existe saldo pendiente"
            );
        }
        if (fechaVencimiento.isBefore(hoy())) {
            throw new SolicitudInvalidaException(
                "La fecha de vencimiento no puede estar en el pasado"
            );
        }
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new SolicitudInvalidaException(
                "La fecha desde no puede ser posterior a la fecha hasta"
            );
        }
    }

    private String normalizarTexto(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }

    private LocalDate hoy() {
        return LocalDate.now(ZONA_NEGOCIO);
    }
}
