package pe.com.proveperu.sgc.devolucion.application.service;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.caja.application.service.CajaService;
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
import pe.com.proveperu.sgc.configuracion.domain.model.MetodoPago;
import pe.com.proveperu.sgc.configuracion.infrastructure.persistence.MetodoPagoRepository;
import pe.com.proveperu.sgc.devolucion.api.dto.CambioDevolucionRequest;
import pe.com.proveperu.sgc.devolucion.api.dto.CambioItemRequest;
import pe.com.proveperu.sgc.devolucion.api.dto.DevolucionCrearRequest;
import pe.com.proveperu.sgc.devolucion.api.dto.DevolucionItemRequest;
import pe.com.proveperu.sgc.devolucion.api.dto.DevolucionResponse;
import pe.com.proveperu.sgc.devolucion.api.dto.DevolucionResumenResponse;
import pe.com.proveperu.sgc.devolucion.api.dto.DescuentoDevolucionRequest;
import pe.com.proveperu.sgc.devolucion.api.dto.ReembolsoDevolucionRequest;
import pe.com.proveperu.sgc.devolucion.domain.model.DetalleCambioDevolucion;
import pe.com.proveperu.sgc.devolucion.domain.model.DetalleDevolucion;
import pe.com.proveperu.sgc.devolucion.domain.model.Devolucion;
import pe.com.proveperu.sgc.devolucion.domain.model.EstadoDevolucion;
import pe.com.proveperu.sgc.devolucion.domain.model.EstadoProductoDevuelto;
import pe.com.proveperu.sgc.devolucion.domain.model.ReembolsoDevolucion;
import pe.com.proveperu.sgc.devolucion.domain.model.TipoSolucionDevolucion;
import pe.com.proveperu.sgc.devolucion.infrastructure.persistence.DetalleDevolucionRepository;
import pe.com.proveperu.sgc.devolucion.infrastructure.persistence.DevolucionRepository;
import pe.com.proveperu.sgc.devolucion.infrastructure.persistence.ReembolsoDevolucionRepository;
import pe.com.proveperu.sgc.inventario.application.service.InventarioService;
import pe.com.proveperu.sgc.security.application.exception.OperacionNoPermitidaException;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;
import pe.com.proveperu.sgc.security.domain.model.EstadoUsuario;
import pe.com.proveperu.sgc.security.domain.model.Usuario;
import pe.com.proveperu.sgc.security.infrastructure.persistence.UsuarioRepository;
import pe.com.proveperu.sgc.shared.api.dto.PaginaResponse;
import pe.com.proveperu.sgc.shared.application.exception.ReglaNegocioException;
import pe.com.proveperu.sgc.shared.application.exception.SolicitudInvalidaException;
import pe.com.proveperu.sgc.venta.domain.model.CuentaCobrar;
import pe.com.proveperu.sgc.venta.domain.model.DetalleVenta;
import pe.com.proveperu.sgc.venta.domain.model.EstadoCuentaCobrar;
import pe.com.proveperu.sgc.venta.domain.model.EstadoVenta;
import pe.com.proveperu.sgc.venta.domain.model.Venta;
import pe.com.proveperu.sgc.venta.infrastructure.persistence.CuentaCobrarRepository;
import pe.com.proveperu.sgc.venta.infrastructure.persistence.VentaRepository;

@Service
@RequiredArgsConstructor
public class DevolucionService {

    private static final int ESCALA_DINERO = 2;
    private static final int ESCALA_CANTIDAD = 3;
    private static final BigDecimal CERO_DINERO = BigDecimal.ZERO.setScale(2);
    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Lima");

    private final DevolucionRepository devolucionRepository;
    private final DetalleDevolucionRepository detalleRepository;
    private final ReembolsoDevolucionRepository reembolsoRepository;
    private final VentaRepository ventaRepository;
    private final CuentaCobrarRepository cuentaRepository;
    private final MetodoPagoRepository metodoPagoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ProductoRepository productoRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;
    private final ProductoUnidadConversionRepository conversionRepository;
    private final PrecioProductoRepository precioRepository;
    private final ClientePrecioEspecialRepository precioEspecialRepository;
    private final InventarioService inventarioService;
    private final CajaService cajaService;

    @Transactional(readOnly = true)
    public PaginaResponse<DevolucionResumenResponse> listar(
        Long idVenta,
        EstadoDevolucion estado,
        TipoSolucionDevolucion tipoSolucion,
        LocalDate desde,
        LocalDate hasta,
        Pageable pageable
    ) {
        validarRango(desde, hasta);
        Page<DevolucionResumenResponse> pagina = devolucionRepository.findAll(
            crearFiltros(idVenta, estado, tipoSolucion, desde, hasta),
            pageable
        ).map(DevolucionResumenResponse::from);
        return PaginaResponse.from(pagina);
    }

    @Transactional(readOnly = true)
    public DevolucionResponse obtener(Long id) {
        return DevolucionResponse.from(buscarDevolucion(id));
    }

    @Transactional
    public DevolucionResponse registrar(
        DevolucionCrearRequest request,
        String usuarioLogin
    ) {
        Venta venta = ventaRepository.findForUpdate(request.idVenta())
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la venta solicitada"
            ));
        validarVentaDevuelta(venta);
        Usuario usuario = buscarUsuarioActivo(usuarioLogin);
        Map<Long, DetalleVenta> detallesVenta = new HashMap<>();
        venta.getDetalles().forEach(detalle -> detallesVenta.put(
            detalle.getId(),
            detalle
        ));
        validarItemsUnicos(request.items());
        validarItemsSegunSolucion(request);

        Devolucion devolucion = new Devolucion();
        devolucion.setVenta(venta);
        devolucion.setUsuario(usuario);
        devolucion.setMotivo(request.motivo().strip());
        devolucion.setTipoSolucion(request.tipoSolucion());

        BigDecimal importeTotal = CERO_DINERO;
        for (DevolucionItemRequest item : request.items()) {
            DetalleVenta detalleVenta = detallesVenta.get(item.idDetalleVenta());
            if (detalleVenta == null) {
                throw new SolicitudInvalidaException(
                    "El detalle de venta " + item.idDetalleVenta()
                        + " no pertenece a la venta solicitada"
                );
            }
            DetalleDevolucion detalle = prepararDetalle(
                venta,
                detalleVenta,
                item
            );
            devolucion.agregarDetalle(detalle);
            importeTotal = importeTotal.add(detalle.getImporteDevolucion());
        }

        devolucion.setImporteTotal(importeTotal);
        devolucion.setImporteAplicadoSaldo(CERO_DINERO);
        devolucion.setImporteReembolsable(CERO_DINERO);
        devolucion.setImporteReembolsado(CERO_DINERO);
        devolucion.setImporteReemplazo(CERO_DINERO);
        devolucion.setImporteCobrado(CERO_DINERO);
        prepararEstadoInicial(devolucion, venta);
        devolucion = devolucionRepository.saveAndFlush(devolucion);

        for (DetalleDevolucion detalle : devolucion.getDetalles()) {
            if (request.tipoSolucion() != TipoSolucionDevolucion.DESCUENTO
                && detalle.getEstadoProducto() == EstadoProductoDevuelto.APTO) {
                inventarioService.registrarDevolucionVenta(
                    venta.getSede(),
                    detalle.getProducto(),
                    detalle.getUnidadMedida(),
                    detalle.getCantidad(),
                    detalle.getCantidadBase(),
                    usuario,
                    devolucion.getId(),
                    venta.getId()
                );
            }
        }

        if (request.tipoSolucion() != TipoSolucionDevolucion.DESCUENTO) {
            venta.setEstado(esDevolucionTotal(venta)
                ? EstadoVenta.DEVUELTA_TOTAL
                : EstadoVenta.DEVUELTA_PARCIAL);
            ventaRepository.saveAndFlush(venta);
        }
        return DevolucionResponse.from(buscarDevolucion(devolucion.getId()));
    }

    @Transactional
    public DevolucionResponse reembolsar(
        Long id,
        ReembolsoDevolucionRequest request,
        String usuarioLogin
    ) {
        Devolucion devolucion = devolucionRepository.findForUpdate(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la devolución solicitada"
            ));
        if (devolucion.getEstado() != EstadoDevolucion.PENDIENTE_REEMBOLSO) {
            throw new OperacionNoPermitidaException(
                "La devolución " + devolucion.getEstado()
                    + " no admite un reembolso"
            );
        }
        if (reembolsoRepository.existsByDevolucionId(id)) {
            throw new OperacionNoPermitidaException(
                "La devolución ya tiene un reembolso registrado"
            );
        }
        BigDecimal pendiente = devolucion.getImporteReembolsable()
            .subtract(devolucion.getImporteReembolsado());
        BigDecimal importe = normalizarDinero(request.importe(), "El importe");
        if (importe.compareTo(pendiente) != 0) {
            throw new ReglaNegocioException(
                "El importe debe coincidir con el reembolso pendiente: "
                    + pendiente.toPlainString()
            );
        }

        MetodoPago metodo = buscarMetodoPagoActivo(request.idMetodoPago());
        Usuario usuario = buscarUsuarioActivo(usuarioLogin);
        ReembolsoDevolucion reembolso = new ReembolsoDevolucion();
        reembolso.setDevolucion(devolucion);
        reembolso.setMetodoPago(metodo);
        reembolso.setUsuario(usuario);
        reembolso.setImporte(importe);
        reembolso.setReferencia(normalizarTexto(request.referencia()));
        reembolso = reembolsoRepository.saveAndFlush(reembolso);

        descontarImportePagado(devolucion.getVenta(), importe);
        distribuirReembolso(devolucion, importe);
        devolucion.setImporteReembolsado(
            devolucion.getImporteReembolsado().add(importe)
        );
        devolucion.setEstado(EstadoDevolucion.REEMBOLSADA);
        devolucion.setReembolso(reembolso);
        devolucionRepository.saveAndFlush(devolucion);

        cajaService.registrarEgresoReembolso(
            devolucion.getVenta(),
            devolucion.getId(),
            reembolso.getId(),
            metodo,
            importe,
            usuario,
            reembolso.getReferencia()
        );
        return DevolucionResponse.from(buscarDevolucion(id));
    }

    @Transactional
    public DevolucionResponse cambiar(
        Long id,
        CambioDevolucionRequest request,
        String usuarioLogin
    ) {
        Devolucion devolucion = devolucionRepository.findForUpdate(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la devolución solicitada"
            ));
        if (devolucion.getTipoSolucion() != TipoSolucionDevolucion.CAMBIO
            || devolucion.getEstado() != EstadoDevolucion.PENDIENTE_CAMBIO) {
            throw new OperacionNoPermitidaException(
                "La devolución no está pendiente de un cambio"
            );
        }
        validarReemplazosUnicos(request.items());
        Usuario usuario = buscarUsuarioActivo(usuarioLogin);
        BigDecimal importeReemplazo = CERO_DINERO;
        for (CambioItemRequest item : request.items()) {
            DetalleCambioDevolucion detalle = prepararDetalleCambio(
                devolucion.getVenta(),
                item
            );
            devolucion.agregarDetalleCambio(detalle);
            importeReemplazo = importeReemplazo.add(detalle.getSubtotal());
        }

        BigDecimal aplicadoSaldo = CERO_DINERO;
        BigDecimal reembolsable = CERO_DINERO;
        BigDecimal cobrado = CERO_DINERO;
        MetodoPago metodo = null;
        int comparacion = importeReemplazo.compareTo(devolucion.getImporteTotal());
        if (comparacion > 0) {
            cobrado = importeReemplazo.subtract(devolucion.getImporteTotal());
            metodo = exigirMetodoPago(
                request.idMetodoPago(),
                "cobrar la diferencia del cambio"
            );
        } else if (comparacion < 0) {
            BigDecimal credito = devolucion.getImporteTotal()
                .subtract(importeReemplazo);
            aplicadoSaldo = aplicarAlSaldoPendiente(
                devolucion.getVenta(),
                credito
            );
            reembolsable = credito.subtract(aplicadoSaldo);
            if (reembolsable.compareTo(BigDecimal.ZERO) > 0) {
                metodo = exigirMetodoPago(
                    request.idMetodoPago(),
                    "devolver la diferencia del cambio"
                );
                descontarImportePagado(devolucion.getVenta(), reembolsable);
            }
        }

        devolucion.setUsuarioResolucion(usuario);
        devolucion.setMetodoPagoResolucion(metodo);
        devolucion.setFechaResolucion(Instant.now());
        devolucion.setReferenciaResolucion(normalizarTexto(request.referencia()));
        devolucion.setImporteReemplazo(importeReemplazo);
        devolucion.setImporteCobrado(cobrado);
        devolucion.setImporteAplicadoSaldo(aplicadoSaldo);
        devolucion.setImporteReembolsable(reembolsable);
        devolucion.setImporteReembolsado(reembolsable);
        devolucion.setEstado(EstadoDevolucion.CAMBIADA);
        distribuirReembolso(devolucion, reembolsable);
        devolucionRepository.saveAndFlush(devolucion);

        devolucion.getDetallesCambio().stream()
            .sorted(Comparator.comparing(detalle -> detalle.getProducto().getId()))
            .forEach(detalle -> inventarioService.registrarSalidaCambio(
                devolucion.getVenta().getSede(),
                detalle.getProducto(),
                detalle.getUnidadMedida(),
                detalle.getCantidad(),
                detalle.getCantidadBase(),
                usuario,
                devolucion.getId()
            ));

        if (cobrado.compareTo(BigDecimal.ZERO) > 0) {
            cajaService.registrarIngresoCambio(
                devolucion.getVenta(),
                devolucion.getId(),
                metodo,
                cobrado,
                usuario,
                devolucion.getReferenciaResolucion()
            );
        } else if (reembolsable.compareTo(BigDecimal.ZERO) > 0) {
            cajaService.registrarEgresoCambio(
                devolucion.getVenta(),
                devolucion.getId(),
                metodo,
                reembolsable,
                usuario,
                devolucion.getReferenciaResolucion()
            );
        }
        return DevolucionResponse.from(buscarDevolucion(id));
    }

    @Transactional
    public DevolucionResponse descontar(
        Long id,
        DescuentoDevolucionRequest request,
        String usuarioLogin
    ) {
        Devolucion devolucion = devolucionRepository.findForUpdate(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la devolución solicitada"
            ));
        if (devolucion.getTipoSolucion() != TipoSolucionDevolucion.DESCUENTO
            || devolucion.getEstado() != EstadoDevolucion.PENDIENTE_DESCUENTO) {
            throw new OperacionNoPermitidaException(
                "La devolución no está pendiente de un descuento"
            );
        }
        BigDecimal importe = normalizarDinero(
            request.importe(),
            "El descuento"
        );
        if (importe.compareTo(devolucion.getImporteTotal()) > 0) {
            throw new ReglaNegocioException(
                "El descuento no puede superar el valor reclamado: "
                    + devolucion.getImporteTotal().toPlainString()
            );
        }

        Usuario usuario = buscarUsuarioActivo(usuarioLogin);
        BigDecimal aplicadoSaldo = aplicarAlSaldoPendiente(
            devolucion.getVenta(),
            importe
        );
        BigDecimal reembolsable = importe.subtract(aplicadoSaldo);
        MetodoPago metodo = null;
        if (reembolsable.compareTo(BigDecimal.ZERO) > 0) {
            metodo = exigirMetodoPago(
                request.idMetodoPago(),
                "entregar el descuento"
            );
            descontarImportePagado(devolucion.getVenta(), reembolsable);
        }

        devolucion.setUsuarioResolucion(usuario);
        devolucion.setMetodoPagoResolucion(metodo);
        devolucion.setFechaResolucion(Instant.now());
        devolucion.setReferenciaResolucion(normalizarTexto(request.referencia()));
        devolucion.setImporteAplicadoSaldo(aplicadoSaldo);
        devolucion.setImporteReembolsable(reembolsable);
        devolucion.setImporteReembolsado(reembolsable);
        devolucion.setEstado(EstadoDevolucion.DESCONTADA);
        distribuirDescuento(devolucion, importe);
        distribuirReembolso(devolucion, reembolsable);
        devolucionRepository.saveAndFlush(devolucion);

        if (reembolsable.compareTo(BigDecimal.ZERO) > 0) {
            cajaService.registrarEgresoDescuento(
                devolucion.getVenta(),
                devolucion.getId(),
                metodo,
                reembolsable,
                usuario,
                devolucion.getReferenciaResolucion()
            );
        }
        return DevolucionResponse.from(buscarDevolucion(id));
    }

    private void prepararEstadoInicial(Devolucion devolucion, Venta venta) {
        switch (devolucion.getTipoSolucion()) {
            case REEMBOLSO -> {
                BigDecimal aplicadoSaldo = aplicarAlSaldoPendiente(
                    venta,
                    devolucion.getImporteTotal()
                );
                BigDecimal reembolsable = devolucion.getImporteTotal()
                    .subtract(aplicadoSaldo);
                devolucion.setImporteAplicadoSaldo(aplicadoSaldo);
                devolucion.setImporteReembolsable(reembolsable);
                devolucion.setEstado(reembolsable.compareTo(BigDecimal.ZERO) > 0
                    ? EstadoDevolucion.PENDIENTE_REEMBOLSO
                    : EstadoDevolucion.COMPLETADA);
            }
            case CAMBIO -> devolucion.setEstado(
                EstadoDevolucion.PENDIENTE_CAMBIO
            );
            case DESCUENTO -> devolucion.setEstado(
                EstadoDevolucion.PENDIENTE_DESCUENTO
            );
        }
    }

    private DetalleDevolucion prepararDetalle(
        Venta venta,
        DetalleVenta detalleVenta,
        DevolucionItemRequest item
    ) {
        BigDecimal cantidad = normalizarCantidad(
            item.cantidad(),
            detalleVenta
        );
        BigDecimal cantidadAnterior = detalleRepository.sumarCantidadDevuelta(
            detalleVenta.getId()
        ).setScale(ESCALA_CANTIDAD);
        BigDecimal cantidadRestante = detalleVenta.getCantidad()
            .subtract(cantidadAnterior);
        if (cantidad.compareTo(cantidadRestante) > 0) {
            throw new ReglaNegocioException(
                "La cantidad del producto "
                    + detalleVenta.getProducto().getCodigoInterno()
                    + " supera lo pendiente de devolver: "
                    + cantidadRestante.toPlainString()
            );
        }

        BigDecimal baseAnterior = detalleRepository.sumarCantidadBaseDevuelta(
            detalleVenta.getId()
        ).setScale(ESCALA_CANTIDAD);
        BigDecimal baseRestante = detalleVenta.getCantidadBase()
            .subtract(baseAnterior);
        BigDecimal cantidadBase = cantidad.compareTo(cantidadRestante) == 0
            ? baseRestante
            : detalleVenta.getCantidadBase()
                .multiply(cantidad)
                .divide(
                    detalleVenta.getCantidad(),
                    ESCALA_CANTIDAD,
                    RoundingMode.HALF_UP
                );
        if (cantidadBase.compareTo(BigDecimal.ZERO) <= 0) {
            throw new SolicitudInvalidaException(
                "La cantidad produce una devolución menor a la precisión admitida"
            );
        }

        BigDecimal importeAnterior = detalleRepository.sumarImporteDevuelto(
            detalleVenta.getId()
        ).setScale(ESCALA_DINERO);
        BigDecimal valorTotalDetalle = valorTotalDetalle(venta, detalleVenta);
        BigDecimal importeRestante = valorTotalDetalle.subtract(importeAnterior);
        BigDecimal importe = cantidad.compareTo(cantidadRestante) == 0
            ? importeRestante
            : valorTotalDetalle
                .multiply(cantidad)
                .divide(
                    detalleVenta.getCantidad(),
                    ESCALA_DINERO,
                    RoundingMode.HALF_UP
                )
                .min(importeRestante);

        DetalleDevolucion detalle = new DetalleDevolucion();
        detalle.setDetalleVenta(detalleVenta);
        detalle.setProducto(detalleVenta.getProducto());
        detalle.setUnidadMedida(detalleVenta.getUnidadMedida());
        detalle.setCantidad(cantidad);
        detalle.setCantidadBase(cantidadBase);
        detalle.setEstadoProducto(item.estadoProducto());
        detalle.setImporteDevolucion(importe);
        detalle.setImporteReembolso(CERO_DINERO);
        detalle.setDescuentoAplicado(CERO_DINERO);
        return detalle;
    }

    private DetalleCambioDevolucion prepararDetalleCambio(
        Venta venta,
        CambioItemRequest item
    ) {
        Producto producto = buscarProductoActivo(item.idProducto());
        UnidadMedida unidad = buscarUnidadActiva(item.idUnidadMedida());
        BigDecimal cantidad = normalizarCantidadCambio(item.cantidad(), unidad);
        BigDecimal factor = factorAUnidadBase(producto, unidad);
        BigDecimal precio = resolverPrecioBase(
            venta.getCliente(),
            producto,
            venta.getTipoVenta().name(),
            LocalDate.now(ZONA_NEGOCIO)
        ).multiply(factor).setScale(ESCALA_DINERO, RoundingMode.HALF_UP);
        if (item.precioUnitario() != null
            && normalizarDinero(item.precioUnitario(), "El precio esperado")
                .compareTo(precio) != 0) {
            throw new OperacionNoPermitidaException(
                "El precio vigente del producto " + producto.getCodigoInterno()
                    + " es " + precio.toPlainString()
            );
        }
        BigDecimal cantidadBase = cantidad.multiply(factor).setScale(
            ESCALA_CANTIDAD,
            RoundingMode.HALF_UP
        );
        if (cantidadBase.compareTo(BigDecimal.ZERO) <= 0) {
            throw new SolicitudInvalidaException(
                "La conversión produce una cantidad menor a la precisión admitida"
            );
        }

        DetalleCambioDevolucion detalle = new DetalleCambioDevolucion();
        detalle.setProducto(producto);
        detalle.setUnidadMedida(unidad);
        detalle.setCantidad(cantidad);
        detalle.setCantidadBase(cantidadBase);
        detalle.setPrecioUnitario(precio);
        detalle.setSubtotal(cantidad.multiply(precio).setScale(
            ESCALA_DINERO,
            RoundingMode.HALF_UP
        ));
        return detalle;
    }

    private BigDecimal valorTotalDetalle(
        Venta venta,
        DetalleVenta detalleBuscado
    ) {
        List<DetalleVenta> detalles = venta.getDetalles().stream()
            .sorted(Comparator.comparing(DetalleVenta::getId))
            .toList();
        BigDecimal asignado = CERO_DINERO;
        for (int indice = 0; indice < detalles.size(); indice++) {
            DetalleVenta detalle = detalles.get(indice);
            BigDecimal valor;
            if (indice == detalles.size() - 1) {
                valor = venta.getTotal().subtract(asignado);
            } else if (venta.getSubtotal().compareTo(BigDecimal.ZERO) == 0) {
                valor = CERO_DINERO;
            } else {
                valor = venta.getTotal()
                    .multiply(detalle.getSubtotal())
                    .divide(
                        venta.getSubtotal(),
                        ESCALA_DINERO,
                        RoundingMode.HALF_UP
                    );
                asignado = asignado.add(valor);
            }
            if (detalle.getId().equals(detalleBuscado.getId())) {
                return valor;
            }
        }
        throw new SolicitudInvalidaException(
            "No se pudo calcular el valor del producto devuelto"
        );
    }

    private BigDecimal aplicarAlSaldoPendiente(
        Venta venta,
        BigDecimal importeTotal
    ) {
        CuentaCobrar cuenta = cuentaRepository.findByVentaIdForUpdate(venta.getId())
            .orElse(null);
        if (cuenta == null) {
            return CERO_DINERO;
        }
        BigDecimal aplicado = importeTotal.min(cuenta.getSaldoPendiente());
        if (aplicado.compareTo(BigDecimal.ZERO) == 0) {
            return CERO_DINERO;
        }
        cuenta.setTotal(cuenta.getTotal().subtract(aplicado));
        cuenta.setSaldoPendiente(cuenta.getSaldoPendiente().subtract(aplicado));
        cuenta.setEstado(calcularEstadoCuenta(cuenta));
        cuentaRepository.saveAndFlush(cuenta);
        return aplicado;
    }

    private void descontarImportePagado(Venta venta, BigDecimal importe) {
        CuentaCobrar cuenta = cuentaRepository.findByVentaIdForUpdate(venta.getId())
            .orElse(null);
        if (cuenta == null) {
            return;
        }
        if (importe.compareTo(cuenta.getImportePagado()) > 0) {
            throw new OperacionNoPermitidaException(
                "El reembolso supera el importe pagado de la cuenta"
            );
        }
        cuenta.setImportePagado(cuenta.getImportePagado().subtract(importe));
        cuenta.setTotal(cuenta.getTotal().subtract(importe));
        cuenta.setEstado(calcularEstadoCuenta(cuenta));
        cuentaRepository.saveAndFlush(cuenta);
    }

    private EstadoCuentaCobrar calcularEstadoCuenta(CuentaCobrar cuenta) {
        if (cuenta.getTotal().compareTo(BigDecimal.ZERO) == 0) {
            return EstadoCuentaCobrar.ANULADO;
        }
        if (cuenta.getSaldoPendiente().compareTo(BigDecimal.ZERO) == 0) {
            return EstadoCuentaCobrar.PAGADO;
        }
        if (cuenta.getFechaVencimiento() != null
            && cuenta.getFechaVencimiento().isBefore(LocalDate.now(ZONA_NEGOCIO))) {
            return EstadoCuentaCobrar.VENCIDO;
        }
        return cuenta.getImportePagado().compareTo(BigDecimal.ZERO) > 0
            ? EstadoCuentaCobrar.PARCIAL
            : EstadoCuentaCobrar.PENDIENTE;
    }

    private void distribuirReembolso(
        Devolucion devolucion,
        BigDecimal importe
    ) {
        BigDecimal restante = importe;
        for (DetalleDevolucion detalle : devolucion.getDetalles()) {
            BigDecimal asignado = detalle.getImporteDevolucion().min(restante);
            detalle.setImporteReembolso(asignado);
            restante = restante.subtract(asignado);
        }
        detalleRepository.saveAll(devolucion.getDetalles());
    }

    private void distribuirDescuento(
        Devolucion devolucion,
        BigDecimal importe
    ) {
        BigDecimal restante = importe;
        for (DetalleDevolucion detalle : devolucion.getDetalles()) {
            BigDecimal asignado = detalle.getImporteDevolucion().min(restante);
            detalle.setDescuentoAplicado(asignado);
            restante = restante.subtract(asignado);
        }
        detalleRepository.saveAll(devolucion.getDetalles());
    }

    private boolean esDevolucionTotal(Venta venta) {
        return venta.getDetalles().stream().allMatch(detalle ->
            detalleRepository.sumarCantidadFisicamenteDevuelta(
                detalle.getId(),
                TipoSolucionDevolucion.DESCUENTO
            )
                .compareTo(detalle.getCantidad()) >= 0
        );
    }

    private BigDecimal normalizarCantidad(
        BigDecimal cantidad,
        DetalleVenta detalleVenta
    ) {
        BigDecimal normalizada;
        try {
            normalizada = cantidad.setScale(
                ESCALA_CANTIDAD,
                RoundingMode.UNNECESSARY
            );
        } catch (ArithmeticException exception) {
            throw new SolicitudInvalidaException(
                "La cantidad admite como máximo 3 decimales"
            );
        }
        if (!detalleVenta.getUnidadMedida().isPermiteDecimales()
            && normalizada.stripTrailingZeros().scale() > 0) {
            throw new SolicitudInvalidaException(
                "La unidad seleccionada no permite cantidades decimales"
            );
        }
        return normalizada;
    }

    private void validarVentaDevuelta(Venta venta) {
        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new OperacionNoPermitidaException(
                "Una venta anulada no admite devoluciones"
            );
        }
        if (venta.getEstado() == EstadoVenta.DEVUELTA_TOTAL) {
            throw new OperacionNoPermitidaException(
                "La venta ya fue devuelta completamente"
            );
        }
    }

    private void validarItemsUnicos(List<DevolucionItemRequest> items) {
        Set<Long> ids = new HashSet<>();
        for (DevolucionItemRequest item : items) {
            if (!ids.add(item.idDetalleVenta())) {
                throw new SolicitudInvalidaException(
                    "No puede repetir un detalle de venta en la misma devolución"
                );
            }
        }
    }

    private void validarItemsSegunSolucion(DevolucionCrearRequest request) {
        if (request.tipoSolucion() != TipoSolucionDevolucion.DESCUENTO) {
            return;
        }
        boolean contieneApto = request.items().stream().anyMatch(item ->
            item.estadoProducto() == EstadoProductoDevuelto.APTO
        );
        if (contieneApto) {
            throw new SolicitudInvalidaException(
                "El descuento postventa solo corresponde a productos defectuosos, dañados o pendientes de evaluación"
            );
        }
    }

    private void validarReemplazosUnicos(List<CambioItemRequest> items) {
        Set<String> claves = new HashSet<>();
        for (CambioItemRequest item : items) {
            String clave = item.idProducto() + ":" + item.idUnidadMedida();
            if (!claves.add(clave)) {
                throw new SolicitudInvalidaException(
                    "No puede repetir el mismo producto y unidad en el cambio"
                );
            }
        }
    }

    private Specification<Devolucion> crearFiltros(
        Long idVenta,
        EstadoDevolucion estado,
        TipoSolucionDevolucion tipoSolucion,
        LocalDate desde,
        LocalDate hasta
    ) {
        return (root, query, builder) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (idVenta != null) {
                condiciones.add(builder.equal(root.get("venta").get("id"), idVenta));
            }
            if (estado != null) {
                condiciones.add(builder.equal(root.get("estado"), estado));
            }
            if (tipoSolucion != null) {
                condiciones.add(builder.equal(root.get("tipoSolucion"), tipoSolucion));
            }
            if (desde != null) {
                condiciones.add(builder.greaterThanOrEqualTo(
                    root.get("fechaHora"),
                    desde.atStartOfDay(ZONA_NEGOCIO).toInstant()
                ));
            }
            if (hasta != null) {
                condiciones.add(builder.lessThan(
                    root.get("fechaHora"),
                    hasta.plusDays(1).atStartOfDay(ZONA_NEGOCIO).toInstant()
                ));
            }
            return builder.and(condiciones.toArray(Predicate[]::new));
        };
    }

    private Devolucion buscarDevolucion(Long id) {
        return devolucionRepository.findDetalleById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la devolución solicitada"
            ));
    }

    private Producto buscarProductoActivo(Long idProducto) {
        Producto producto = productoRepository.findByIdWithReferencias(idProducto)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe el producto de reemplazo solicitado"
            ));
        if (producto.getEstado() != EstadoCatalogo.ACTIVO) {
            throw new OperacionNoPermitidaException(
                "El producto de reemplazo no está activo"
            );
        }
        return producto;
    }

    private UnidadMedida buscarUnidadActiva(Long idUnidad) {
        UnidadMedida unidad = unidadMedidaRepository.findById(idUnidad)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la unidad de medida solicitada"
            ));
        if (unidad.getEstado() != EstadoCatalogo.ACTIVO) {
            throw new OperacionNoPermitidaException(
                "La unidad de medida seleccionada no está activa"
            );
        }
        return unidad;
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
                "No existe un precio " + tipoPrecio
                    + " vigente para el producto "
                    + producto.getCodigoInterno()
            );
        }
        return precios.getFirst().getMonto();
    }

    private BigDecimal factorAUnidadBase(
        Producto producto,
        UnidadMedida unidad
    ) {
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

    private BigDecimal normalizarCantidadCambio(
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
                "La unidad " + unidad.getCodigo()
                    + " no permite cantidades decimales"
            );
        }
        return cantidad;
    }

    private MetodoPago exigirMetodoPago(Long idMetodoPago, String operacion) {
        if (idMetodoPago == null) {
            throw new SolicitudInvalidaException(
                "El método de pago es obligatorio para " + operacion
            );
        }
        return buscarMetodoPagoActivo(idMetodoPago);
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

    private BigDecimal normalizarDinero(BigDecimal valor, String campo) {
        try {
            return valor.setScale(ESCALA_DINERO, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new SolicitudInvalidaException(
                campo + " admite como máximo 2 decimales"
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

    private String normalizarTexto(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }
}
