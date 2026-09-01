package pe.com.proveperu.sgc.inventario.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.catalogo.domain.model.PresentacionProducto;
import pe.com.proveperu.sgc.catalogo.domain.model.Producto;
import pe.com.proveperu.sgc.catalogo.domain.model.ProductoUnidadConversion;
import pe.com.proveperu.sgc.catalogo.domain.model.UnidadMedida;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.ProductoRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.PresentacionProductoRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.ProductoUnidadConversionRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.UnidadMedidaRepository;
import pe.com.proveperu.sgc.inventario.api.dto.AjusteInventarioRequest;
import pe.com.proveperu.sgc.inventario.api.dto.AjusteInventarioResponse;
import pe.com.proveperu.sgc.inventario.api.dto.MovimientoInventarioResponse;
import pe.com.proveperu.sgc.inventario.api.dto.ExistenciaPresentacionResponse;
import pe.com.proveperu.sgc.inventario.api.dto.IngresoPresentacionesRequest;
import pe.com.proveperu.sgc.inventario.api.dto.IngresoPresentacionesResponse;
import pe.com.proveperu.sgc.inventario.api.dto.StockInventarioResponse;
import pe.com.proveperu.sgc.inventario.api.dto.StockMinimoInventarioRequest;
import pe.com.proveperu.sgc.inventario.api.dto.TransferenciaInventarioRequest;
import pe.com.proveperu.sgc.inventario.api.dto.TransferenciaInventarioResponse;
import pe.com.proveperu.sgc.inventario.domain.model.Inventario;
import pe.com.proveperu.sgc.inventario.domain.model.ConsumoExistenciaPresentacion;
import pe.com.proveperu.sgc.inventario.domain.model.EstadoExistenciaPresentacion;
import pe.com.proveperu.sgc.inventario.domain.model.ExistenciaPresentacion;
import pe.com.proveperu.sgc.inventario.domain.model.MovimientoInventario;
import pe.com.proveperu.sgc.inventario.domain.model.Sede;
import pe.com.proveperu.sgc.inventario.domain.model.TipoAjusteInventario;
import pe.com.proveperu.sgc.inventario.domain.model.TipoMovimientoInventario;
import pe.com.proveperu.sgc.inventario.domain.model.TransferenciaInventario;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.InventarioRepository;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.ConsumoExistenciaPresentacionRepository;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.ExistenciaPresentacionRepository;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.MovimientoInventarioRepository;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.SedeRepository;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.TransferenciaInventarioRepository;
import pe.com.proveperu.sgc.security.application.exception.OperacionNoPermitidaException;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;
import pe.com.proveperu.sgc.security.domain.model.EstadoUsuario;
import pe.com.proveperu.sgc.security.domain.model.Usuario;
import pe.com.proveperu.sgc.security.infrastructure.persistence.UsuarioRepository;
import pe.com.proveperu.sgc.shared.api.dto.PaginaResponse;
import pe.com.proveperu.sgc.shared.application.exception.ReglaNegocioException;
import pe.com.proveperu.sgc.shared.application.exception.SolicitudInvalidaException;
import pe.com.proveperu.sgc.compra.domain.model.RecepcionCompra;
import pe.com.proveperu.sgc.venta.domain.model.DetalleVenta;

@Service
@RequiredArgsConstructor
public class InventarioService {

    private static final int ESCALA_STOCK = 3;
    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Lima");
    private static final Instant INICIO_CONSULTAS = Instant.parse("1970-01-01T00:00:00Z");
    private static final Instant FIN_CONSULTAS = Instant.parse("9999-12-31T23:59:59Z");

    private final SedeRepository sedeRepository;
    private final ProductoRepository productoRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;
    private final ProductoUnidadConversionRepository conversionRepository;
    private final PresentacionProductoRepository presentacionProductoRepository;
    private final InventarioRepository inventarioRepository;
    private final ExistenciaPresentacionRepository existenciaPresentacionRepository;
    private final ConsumoExistenciaPresentacionRepository consumoPresentacionRepository;
    private final MovimientoInventarioRepository movimientoRepository;
    private final TransferenciaInventarioRepository transferenciaRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public PaginaResponse<StockInventarioResponse> listar(
        Long idSede,
        String buscar,
        Pageable pageable
    ) {
        Sede sede = resolverSede(idSede);
        String criterio = normalizarBusqueda(buscar);
        Page<Producto> productos = productoRepository.buscar(
            criterio,
            EstadoCatalogo.ACTIVO,
            null,
            pageable
        );
        return mapearPaginaStock(sede, productos);
    }

    @Transactional(readOnly = true)
    public StockInventarioResponse obtener(Long idProducto, Long idSede) {
        Sede sede = resolverSede(idSede);
        Producto producto = buscarProducto(idProducto, false);
        Inventario inventario = inventarioRepository
            .findBySedeIdAndProductoId(sede.getId(), producto.getId())
            .orElse(null);
        return StockInventarioResponse.from(sede, producto, inventario);
    }

    @Transactional(readOnly = true)
    public List<ExistenciaPresentacionResponse> listarPresentaciones(
        Long idSede,
        Long idProducto,
        EstadoExistenciaPresentacion estado
    ) {
        Sede sede = resolverSede(idSede);
        buscarProducto(idProducto, false);
        var estados = estado == null
            ? EnumSet.allOf(EstadoExistenciaPresentacion.class)
            : EnumSet.of(estado);
        return existenciaPresentacionRepository
            .findAllBySede_IdAndPresentacion_Producto_IdAndEstadoInOrderByFechaIngresoAscIdAsc(
                sede.getId(), idProducto, estados)
            .stream()
            .map(ExistenciaPresentacionResponse::from)
            .toList();
    }

    @Transactional
    public IngresoPresentacionesResponse registrarPresentaciones(
        IngresoPresentacionesRequest request,
        String usuarioLogin
    ) {
        Sede sede = resolverSede(request.idSede());
        Producto producto = buscarProducto(request.idProducto(), true);
        PresentacionProducto presentacion = buscarPresentacionActiva(
            request.idPresentacionProducto(), producto.getId()
        );
        Usuario usuario = buscarUsuarioActivo(usuarioLogin);
        List<BigDecimal> contenidos = resolverContenidosIngreso(
            producto,
            presentacion,
            request.cantidadBultos(),
            request.contenidosBase()
        );
        BigDecimal cantidadBase = contenidos.stream()
            .reduce(BigDecimal.ZERO.setScale(ESCALA_STOCK), BigDecimal::add);
        Inventario inventario = inventarioRepository
            .findForUpdate(sede.getId(), producto.getId())
            .orElseThrow(() -> new ReglaNegocioException(
                "Primero registra la mercadería en " + sede.getNombre()
            ));
        BigDecimal stockNoVinculado = calcularStockNoVinculado(
            sede, producto, inventario
        );
        if (cantidadBase.compareTo(stockNoVinculado) > 0) {
            throw new ReglaNegocioException(
                "Solo hay " + stockNoVinculado.toPlainString() + " "
                    + producto.getUnidadBase().getCodigo()
                    + " sin vincular a bultos en " + sede.getNombre()
            );
        }

        Instant ahora = Instant.now();
        List<ExistenciaPresentacion> existencias = crearExistenciasPresentacion(
            sede, presentacion, contenidos, null, ahora
        );
        MovimientoInventario movimiento = registrarConversionBultos(
            sede,
            producto,
            presentacion,
            usuario,
            contenidos.size(),
            cantidadBase,
            inventario.getStockFisico(),
            request.motivo().strip(),
            ahora
        );
        return new IngresoPresentacionesResponse(
            existencias.stream()
                .map(ExistenciaPresentacionResponse::from)
                .toList(),
            MovimientoInventarioResponse.from(movimiento),
            StockInventarioResponse.from(sede, producto, inventario)
        );
    }

    @Transactional
    public ExistenciaPresentacionResponse abrirPresentacion(
        Long id,
        String usuarioLogin
    ) {
        buscarUsuarioActivo(usuarioLogin);
        ExistenciaPresentacion existencia = existenciaPresentacionRepository
            .findForUpdate(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la caja, paquete o rollo solicitado"
            ));
        if (existencia.getEstado() != EstadoExistenciaPresentacion.CERRADO) {
            throw new OperacionNoPermitidaException(
                "Solo se puede abrir una presentación que permanece cerrada"
            );
        }
        existencia.setEstado(EstadoExistenciaPresentacion.ABIERTO);
        existencia.setFechaApertura(Instant.now());
        return ExistenciaPresentacionResponse.from(existencia);
    }

    @Transactional(readOnly = true)
    public ExistenciaPresentacion buscarPresentacionParaVenta(
        Long id,
        Sede sede,
        Producto producto
    ) {
        ExistenciaPresentacion existencia = existenciaPresentacionRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la presentación física seleccionada"
            ));
        if (!existencia.getSede().getId().equals(sede.getId())
            || !existencia.getPresentacion().getProducto().getId().equals(producto.getId())) {
            throw new SolicitudInvalidaException(
                "La presentación seleccionada no corresponde al producto y almacén de la venta"
            );
        }
        if (existencia.getEstado() != EstadoExistenciaPresentacion.CERRADO) {
            throw new OperacionNoPermitidaException(
                "La presentación seleccionada ya fue abierta o agotada"
            );
        }
        return existencia;
    }

    @Transactional
    public void consumirPresentacionesVenta(Sede sede, DetalleVenta detalle) {
        BigDecimal requerida = detalle.getCantidadBase().setScale(
            ESCALA_STOCK, RoundingMode.UNNECESSARY
        );
        Inventario inventario = inventarioRepository
            .findForUpdate(sede.getId(), detalle.getProducto().getId())
            .orElseThrow(() -> new ReglaNegocioException("Stock insuficiente. Disponible: 0.000"));
        if (requerida.compareTo(inventario.getStockDisponible()) > 0) {
            throw new ReglaNegocioException(
                "Stock insuficiente. Disponible: "
                    + inventario.getStockDisponible().toPlainString()
            );
        }
        if (detalle.getExistenciaPresentacion() != null) {
            if (!detalle.getExistenciaPresentacion().getPresentacion().isContenidoVariable()) {
                consumirPresentacionesFijas(sede, detalle, requerida);
                return;
            }
            ExistenciaPresentacion existencia = existenciaPresentacionRepository
                .findForUpdate(detalle.getExistenciaPresentacion().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                    "No existe la presentación física seleccionada"
                ));
            if (existencia.getEstado() != EstadoExistenciaPresentacion.CERRADO
                || existencia.getCantidadDisponibleBase().compareTo(requerida) != 0) {
                throw new OperacionNoPermitidaException(
                    "La presentación completa ya no se encuentra disponible"
                );
            }
            agotar(existencia);
            registrarConsumo(detalle, existencia, requerida);
            return;
        }

        consumirPresentacionesFraccionadas(
            sede, detalle, requerida, inventario.getStockDisponible()
        );
    }

    private void consumirPresentacionesFijas(
        Sede sede,
        DetalleVenta detalle,
        BigDecimal requerida
    ) {
        int cantidadBultos;
        try {
            cantidadBultos = detalle.getCantidad().intValueExact();
        } catch (ArithmeticException exception) {
            throw new SolicitudInvalidaException(
                "La cantidad de cajas, paquetes o rollos debe ser un número entero"
            );
        }
        var presentacion = detalle.getExistenciaPresentacion().getPresentacion();
        List<ExistenciaPresentacion> disponibles = existenciaPresentacionRepository
            .findAllForUpdateByPresentacion(
                sede.getId(), presentacion.getId(), EstadoExistenciaPresentacion.CERRADO
            );
        if (disponibles.size() < cantidadBultos) {
            throw new ReglaNegocioException(
                "Stock insuficiente de " + presentacion.getNombre() + ". Disponibles: "
                    + disponibles.size()
            );
        }
        List<ExistenciaPresentacion> seleccionadas = disponibles.subList(0, cantidadBultos);
        BigDecimal totalSeleccionado = seleccionadas.stream()
            .map(ExistenciaPresentacion::getCantidadDisponibleBase)
            .reduce(BigDecimal.ZERO.setScale(ESCALA_STOCK), BigDecimal::add);
        if (totalSeleccionado.compareTo(requerida) != 0) {
            throw new OperacionNoPermitidaException(
                "El contenido físico de los bultos no coincide con la presentación fija"
            );
        }
        detalle.setExistenciaPresentacion(seleccionadas.getFirst());
        for (ExistenciaPresentacion existencia : seleccionadas) {
            BigDecimal contenido = existencia.getCantidadDisponibleBase();
            agotar(existencia);
            registrarConsumo(detalle, existencia, contenido);
        }
    }

    @Transactional
    public void consumirPresentacionesVentaReservada(
        Sede sede,
        DetalleVenta detalle
    ) {
        BigDecimal requerida = detalle.getCantidadBase().setScale(
            ESCALA_STOCK, RoundingMode.UNNECESSARY
        );
        Inventario inventario = inventarioRepository
            .findForUpdate(sede.getId(), detalle.getProducto().getId())
            .orElseThrow(() -> new ReglaNegocioException("Stock reservado inexistente"));
        if (requerida.compareTo(inventario.getStockReservado()) > 0) {
            throw new OperacionNoPermitidaException(
                "La reserva supera el stock reservado del producto "
                    + detalle.getProducto().getCodigoInterno()
            );
        }
        consumirPresentacionesFraccionadas(
            sede, detalle, requerida, inventario.getStockFisico()
        );
    }

    @Transactional
    public void restaurarPresentacionesVenta(Long idVenta) {
        for (ConsumoExistenciaPresentacion consumo : consumoPresentacionRepository
            .findAllByDetalleVentaVentaIdOrderByIdAsc(idVenta)) {
            ExistenciaPresentacion existencia = existenciaPresentacionRepository
                .findForUpdate(consumo.getExistencia().getId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                    "No existe la presentación consumida por la venta"
                ));
            existencia.setCantidadDisponibleBase(
                existencia.getCantidadDisponibleBase().add(consumo.getCantidadBase())
            );
            boolean fueVentaCompleta = consumo.getDetalleVenta()
                .getExistenciaPresentacion() != null;
            existencia.setEstado(fueVentaCompleta
                ? EstadoExistenciaPresentacion.CERRADO
                : EstadoExistenciaPresentacion.ABIERTO);
            existencia.setFechaAgotamiento(null);
            if (fueVentaCompleta) existencia.setFechaApertura(null);
        }
    }

    @Transactional(readOnly = true)
    public PaginaResponse<StockInventarioResponse> listarStockBajo(
        Long idSede,
        String buscar,
        Pageable pageable
    ) {
        Sede sede = resolverSede(idSede);
        Page<Producto> productos = inventarioRepository.buscarProductosConStockBajo(
            sede.getId(),
            EstadoCatalogo.ACTIVO,
            normalizarBusqueda(buscar),
            pageable
        );
        return mapearPaginaStock(sede, productos);
    }

    @Transactional
    public AjusteInventarioResponse ajustar(
        AjusteInventarioRequest request,
        String usuarioLogin
    ) {
        Sede sede = resolverSede(request.idSede());
        Producto producto = buscarProducto(request.idProducto(), true);
        UnidadMedida unidad = buscarUnidad(request.idUnidadMedida());
        Usuario usuario = buscarUsuarioActivo(usuarioLogin);
        validarCantidadPermitida(unidad, request.cantidad());

        BigDecimal cantidad = request.cantidad().setScale(ESCALA_STOCK, RoundingMode.UNNECESSARY);
        BigDecimal cantidadBase = convertirAUnidadBase(producto, unidad, cantidad);
        if (cantidadBase.compareTo(BigDecimal.ZERO) <= 0) {
            throw new SolicitudInvalidaException(
                "La conversión produce una cantidad menor a la precisión admitida"
            );
        }

        Inventario inventario = inventarioRepository
            .findForUpdate(sede.getId(), producto.getId())
            .orElseGet(() -> crearInventarioVacio(sede, producto));
        BigDecimal stockAnterior = inventario.getStockFisico();
        BigDecimal cantidadFirmada = firmar(request.tipoAjuste(), cantidad);
        BigDecimal cantidadBaseFirmada = firmar(request.tipoAjuste(), cantidadBase);

        if (request.tipoAjuste() == TipoAjusteInventario.SALIDA
            && cantidadBase.compareTo(inventario.getStockDisponible()) > 0) {
            throw new ReglaNegocioException(
                "Stock insuficiente. Disponible: " + inventario.getStockDisponible().toPlainString()
            );
        }
        if (request.tipoAjuste() == TipoAjusteInventario.SALIDA) {
            consumirSueltoYPresentacionesAbiertas(
                sede, producto, cantidadBase, inventario
            );
        }

        BigDecimal stockResultante = stockAnterior.add(cantidadBaseFirmada);
        Instant ahora = Instant.now();
        inventario.setStockFisico(stockResultante);
        inventario.setFechaActualizacion(ahora);
        inventario = inventarioRepository.save(inventario);

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setSede(sede);
        movimiento.setProducto(producto);
        movimiento.setUsuario(usuario);
        movimiento.setUnidadMedida(unidad);
        movimiento.setTipoMovimiento(tipoMovimiento(request.tipoAjuste()));
        movimiento.setCantidad(cantidadFirmada);
        movimiento.setCantidadBase(cantidadBaseFirmada);
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockResultante(stockResultante);
        movimiento.setMotivo(request.motivo().strip());
        movimiento.setFechaHora(ahora);
        movimiento = movimientoRepository.save(movimiento);

        return new AjusteInventarioResponse(
            MovimientoInventarioResponse.from(movimiento),
            StockInventarioResponse.from(sede, producto, inventario)
        );
    }

    @Transactional
    public TransferenciaInventarioResponse transferir(
        TransferenciaInventarioRequest request,
        String usuarioLogin
    ) {
        if (request.idSedeOrigen().equals(request.idSedeDestino())) {
            throw new SolicitudInvalidaException(
                "Los almacenes de origen y destino deben ser diferentes"
            );
        }

        Sede origen = resolverSede(request.idSedeOrigen());
        Sede destino = resolverSede(request.idSedeDestino());
        if (!origen.getIdEmpresa().equals(destino.getIdEmpresa())) {
            throw new OperacionNoPermitidaException(
                "No se puede transferir mercadería entre empresas diferentes"
            );
        }
        Producto producto = buscarProducto(request.idProducto(), true);
        UnidadMedida unidad = buscarUnidad(request.idUnidadMedida());
        Usuario usuario = buscarUsuarioActivo(usuarioLogin);
        validarCantidadPermitida(unidad, request.cantidad());

        BigDecimal cantidad = request.cantidad().setScale(
            ESCALA_STOCK,
            RoundingMode.UNNECESSARY
        );
        BigDecimal cantidadBase = convertirAUnidadBase(producto, unidad, cantidad);
        if (cantidadBase.compareTo(BigDecimal.ZERO) <= 0) {
            throw new SolicitudInvalidaException(
                "La conversión produce una cantidad menor a la precisión admitida"
            );
        }

        Inventario inventarioOrigen = inventarioRepository
            .findForUpdate(origen.getId(), producto.getId())
            .orElseThrow(() -> new ReglaNegocioException(
                "Stock insuficiente en " + origen.getNombre() + ". Disponible: 0.000"
            ));
        if (cantidadBase.compareTo(inventarioOrigen.getStockDisponible()) > 0) {
            throw new ReglaNegocioException(
                "Stock insuficiente en " + origen.getNombre() + ". Disponible: "
                    + inventarioOrigen.getStockDisponible().toPlainString()
            );
        }
        consumirSueltoYPresentacionesAbiertas(
            origen, producto, cantidadBase, inventarioOrigen
        );

        Inventario inventarioDestino = inventarioRepository
            .findForUpdate(destino.getId(), producto.getId())
            .orElseGet(() -> crearInventarioVacio(destino, producto));
        BigDecimal stockOrigenAnterior = inventarioOrigen.getStockFisico();
        BigDecimal stockDestinoAnterior = inventarioDestino.getStockFisico();
        BigDecimal stockOrigenResultante = stockOrigenAnterior.subtract(cantidadBase);
        BigDecimal stockDestinoResultante = stockDestinoAnterior.add(cantidadBase);
        validarCapacidadStock(stockDestinoResultante);

        Instant ahora = Instant.now();
        inventarioOrigen.setStockFisico(stockOrigenResultante);
        inventarioOrigen.setFechaActualizacion(ahora);
        inventarioDestino.setStockFisico(stockDestinoResultante);
        inventarioDestino.setFechaActualizacion(ahora);
        inventarioRepository.save(inventarioOrigen);
        inventarioRepository.save(inventarioDestino);

        TransferenciaInventario transferencia = new TransferenciaInventario();
        transferencia.setSedeOrigen(origen);
        transferencia.setSedeDestino(destino);
        transferencia.setProducto(producto);
        transferencia.setUnidadMedida(unidad);
        transferencia.setUsuario(usuario);
        transferencia.setCantidad(cantidad);
        transferencia.setCantidadBase(cantidadBase);
        transferencia.setMotivo(request.motivo().strip());
        transferencia.setFechaHora(ahora);
        transferencia = transferenciaRepository.save(transferencia);

        MovimientoInventario salida = crearMovimientoTransferencia(
            origen,
            producto,
            unidad,
            usuario,
            TipoMovimientoInventario.TRANSFERENCIA_SALIDA,
            cantidad.negate(),
            cantidadBase.negate(),
            stockOrigenAnterior,
            stockOrigenResultante,
            transferencia.getId(),
            "Traslado a " + destino.getNombre() + ": " + transferencia.getMotivo(),
            ahora
        );
        MovimientoInventario entrada = crearMovimientoTransferencia(
            destino,
            producto,
            unidad,
            usuario,
            TipoMovimientoInventario.TRANSFERENCIA_ENTRADA,
            cantidad,
            cantidadBase,
            stockDestinoAnterior,
            stockDestinoResultante,
            transferencia.getId(),
            "Traslado desde " + origen.getNombre() + ": " + transferencia.getMotivo(),
            ahora
        );

        return TransferenciaInventarioResponse.from(
            transferencia,
            MovimientoInventarioResponse.from(salida),
            MovimientoInventarioResponse.from(entrada),
            StockInventarioResponse.from(origen, producto, inventarioOrigen),
            StockInventarioResponse.from(destino, producto, inventarioDestino)
        );
    }

    @Transactional
    public StockInventarioResponse actualizarStockMinimo(
        Long idProducto,
        StockMinimoInventarioRequest request
    ) {
        Sede sede = resolverSede(request.idSede());
        Producto producto = buscarProducto(idProducto, true);
        BigDecimal minimo = request.stockMinimo().setScale(
            ESCALA_STOCK,
            RoundingMode.UNNECESSARY
        );
        Inventario inventario = inventarioRepository
            .findForUpdate(sede.getId(), producto.getId())
            .orElseGet(() -> crearInventarioVacio(sede, producto));
        inventario.setStockMinimo(minimo);
        inventario.setFechaActualizacion(Instant.now());
        inventario = inventarioRepository.save(inventario);
        return StockInventarioResponse.from(sede, producto, inventario);
    }

    @Transactional
    public MovimientoInventario registrarEntradaCompra(
        Sede sede,
        Producto producto,
        UnidadMedida unidad,
        BigDecimal cantidadRecibida,
        Usuario usuario,
        Long idRecepcion,
        Long idCompra
    ) {
        if (!"ACTIVO".equalsIgnoreCase(sede.getEstado())) {
            throw new OperacionNoPermitidaException(
                "No se puede recibir mercadería en una sede inactiva"
            );
        }
        validarCantidadPermitida(unidad, cantidadRecibida);
        BigDecimal cantidad = cantidadRecibida.setScale(
            ESCALA_STOCK,
            RoundingMode.UNNECESSARY
        );
        BigDecimal cantidadBase = convertirAUnidadBase(producto, unidad, cantidad);
        if (cantidadBase.compareTo(BigDecimal.ZERO) <= 0) {
            throw new SolicitudInvalidaException(
                "La conversión produce una cantidad menor a la precisión admitida"
            );
        }

        Inventario inventario = inventarioRepository
            .findForUpdate(sede.getId(), producto.getId())
            .orElseGet(() -> crearInventarioVacio(sede, producto));
        BigDecimal stockAnterior = inventario.getStockFisico();
        BigDecimal stockResultante = stockAnterior.add(cantidadBase);
        validarCapacidadStock(stockResultante);

        Instant ahora = Instant.now();
        inventario.setStockFisico(stockResultante);
        inventario.setFechaActualizacion(ahora);
        inventarioRepository.save(inventario);

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setSede(sede);
        movimiento.setProducto(producto);
        movimiento.setUsuario(usuario);
        movimiento.setUnidadMedida(unidad);
        movimiento.setTipoMovimiento(TipoMovimientoInventario.COMPRA);
        movimiento.setCantidad(cantidad);
        movimiento.setCantidadBase(cantidadBase);
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockResultante(stockResultante);
        movimiento.setDocumentoOrigen("RECEPCION_COMPRA");
        movimiento.setIdOrigen(idRecepcion);
        movimiento.setMotivo("Recepción confirmada de la compra #" + idCompra);
        movimiento.setFechaHora(ahora);
        return movimientoRepository.save(movimiento);
    }

    @Transactional
    public MovimientoInventario registrarEntradaCompraPresentaciones(
        Sede sede,
        Producto producto,
        PresentacionProducto presentacion,
        List<BigDecimal> contenidosSolicitados,
        Usuario usuario,
        RecepcionCompra recepcion,
        Long idCompra
    ) {
        List<BigDecimal> contenidos = normalizarContenidos(
            producto, presentacion, contenidosSolicitados
        );
        return registrarEntradaPresentaciones(
            sede, producto, presentacion, contenidos, usuario, recepcion, idCompra,
            "Recepción confirmada de la compra #" + idCompra,
            TipoMovimientoInventario.COMPRA
        ).movimiento();
    }

    @Transactional
    public MovimientoInventario reservarParaPedido(
        Sede sede,
        Producto producto,
        UnidadMedida unidad,
        BigDecimal cantidad,
        BigDecimal cantidadBase,
        Usuario usuario,
        Long idPedido
    ) {
        Inventario inventario = inventarioRepository
            .findForUpdate(sede.getId(), producto.getId())
            .orElseThrow(() -> new ReglaNegocioException(
                "Stock insuficiente para " + producto.getCodigoInterno()
                    + ". Disponible: 0.000"
            ));
        BigDecimal cantidadNormalizada = cantidad.setScale(
            ESCALA_STOCK,
            RoundingMode.UNNECESSARY
        );
        BigDecimal cantidadBaseNormalizada = cantidadBase.setScale(
            ESCALA_STOCK,
            RoundingMode.UNNECESSARY
        );
        BigDecimal disponibleAnterior = inventario.getStockDisponible();
        if (cantidadBaseNormalizada.compareTo(disponibleAnterior) > 0) {
            throw new ReglaNegocioException(
                "Stock insuficiente para " + producto.getCodigoInterno()
                    + ". Disponible: " + disponibleAnterior.toPlainString()
            );
        }
        BigDecimal contenidoCerrado = existenciaPresentacionRepository.findAllForUpdate(
                sede.getId(), producto.getId(),
                EnumSet.of(EstadoExistenciaPresentacion.CERRADO)
            ).stream()
            .map(ExistenciaPresentacion::getCantidadDisponibleBase)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal disponibleFraccionado = disponibleAnterior.subtract(contenidoCerrado)
            .max(BigDecimal.ZERO);
        if (cantidadBaseNormalizada.compareTo(disponibleFraccionado) > 0) {
            throw new ReglaNegocioException(
                "Debe abrir una caja, paquete o rollo antes de reservar "
                    + cantidadBaseNormalizada.toPlainString() + " "
                    + producto.getUnidadBase().getCodigo()
            );
        }

        Instant ahora = Instant.now();
        BigDecimal disponibleResultante = disponibleAnterior.subtract(
            cantidadBaseNormalizada
        );
        inventario.setStockReservado(
            inventario.getStockReservado().add(cantidadBaseNormalizada)
        );
        inventario.setFechaActualizacion(ahora);
        inventarioRepository.save(inventario);

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setSede(sede);
        movimiento.setProducto(producto);
        movimiento.setUsuario(usuario);
        movimiento.setUnidadMedida(unidad);
        movimiento.setTipoMovimiento(TipoMovimientoInventario.RESERVA);
        movimiento.setCantidad(cantidadNormalizada);
        movimiento.setCantidadBase(cantidadBaseNormalizada);
        movimiento.setStockAnterior(disponibleAnterior);
        movimiento.setStockResultante(disponibleResultante);
        movimiento.setDocumentoOrigen("PEDIDO");
        movimiento.setIdOrigen(idPedido);
        movimiento.setMotivo("Reserva confirmada para el pedido #" + idPedido);
        movimiento.setFechaHora(ahora);
        return movimientoRepository.save(movimiento);
    }

    @Transactional
    public MovimientoInventario liberarReservaDePedido(
        Sede sede,
        Producto producto,
        UnidadMedida unidad,
        BigDecimal cantidad,
        BigDecimal cantidadBase,
        Usuario usuario,
        Long idPedido
    ) {
        Inventario inventario = inventarioRepository
            .findForUpdate(sede.getId(), producto.getId())
            .orElseThrow(() -> new OperacionNoPermitidaException(
                "No existe inventario para liberar la reserva del producto "
                    + producto.getCodigoInterno()
            ));
        BigDecimal cantidadNormalizada = cantidad.setScale(
            ESCALA_STOCK,
            RoundingMode.UNNECESSARY
        );
        BigDecimal cantidadBaseNormalizada = cantidadBase.setScale(
            ESCALA_STOCK,
            RoundingMode.UNNECESSARY
        );
        if (cantidadBaseNormalizada.compareTo(inventario.getStockReservado()) > 0) {
            throw new OperacionNoPermitidaException(
                "La reserva registrada supera el stock reservado del producto "
                    + producto.getCodigoInterno()
            );
        }

        Instant ahora = Instant.now();
        BigDecimal disponibleAnterior = inventario.getStockDisponible();
        BigDecimal disponibleResultante = disponibleAnterior.add(
            cantidadBaseNormalizada
        );
        inventario.setStockReservado(
            inventario.getStockReservado().subtract(cantidadBaseNormalizada)
        );
        inventario.setFechaActualizacion(ahora);
        inventarioRepository.save(inventario);

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setSede(sede);
        movimiento.setProducto(producto);
        movimiento.setUsuario(usuario);
        movimiento.setUnidadMedida(unidad);
        movimiento.setTipoMovimiento(TipoMovimientoInventario.LIBERACION_RESERVA);
        movimiento.setCantidad(cantidadNormalizada.negate());
        movimiento.setCantidadBase(cantidadBaseNormalizada.negate());
        movimiento.setStockAnterior(disponibleAnterior);
        movimiento.setStockResultante(disponibleResultante);
        movimiento.setDocumentoOrigen("PEDIDO");
        movimiento.setIdOrigen(idPedido);
        movimiento.setMotivo("Reserva liberada por cancelación del pedido #" + idPedido);
        movimiento.setFechaHora(ahora);
        return movimientoRepository.save(movimiento);
    }

    @Transactional
    public MovimientoInventario registrarVentaDirecta(
        Sede sede,
        Producto producto,
        UnidadMedida unidad,
        BigDecimal cantidad,
        BigDecimal cantidadBase,
        Usuario usuario,
        Long idVenta
    ) {
        return registrarSalidaVenta(
            sede,
            producto,
            unidad,
            cantidad,
            cantidadBase,
            usuario,
            idVenta,
            null
        );
    }

    @Transactional
    public MovimientoInventario consumirReservaParaVenta(
        Sede sede,
        Producto producto,
        UnidadMedida unidad,
        BigDecimal cantidad,
        BigDecimal cantidadBase,
        Usuario usuario,
        Long idVenta,
        Long idPedido
    ) {
        return registrarSalidaVenta(
            sede,
            producto,
            unidad,
            cantidad,
            cantidadBase,
            usuario,
            idVenta,
            idPedido
        );
    }

    @Transactional
    public MovimientoInventario restaurarVentaAnulada(
        Sede sede,
        Producto producto,
        UnidadMedida unidad,
        BigDecimal cantidad,
        BigDecimal cantidadBase,
        Usuario usuario,
        Long idVenta
    ) {
        Inventario inventario = inventarioRepository
            .findForUpdate(sede.getId(), producto.getId())
            .orElseGet(() -> crearInventarioVacio(sede, producto));
        BigDecimal cantidadNormalizada = cantidad.setScale(
            ESCALA_STOCK,
            RoundingMode.UNNECESSARY
        );
        BigDecimal cantidadBaseNormalizada = cantidadBase.setScale(
            ESCALA_STOCK,
            RoundingMode.UNNECESSARY
        );
        BigDecimal stockAnterior = inventario.getStockFisico();
        BigDecimal stockResultante = stockAnterior.add(cantidadBaseNormalizada);
        validarCapacidadStock(stockResultante);

        Instant ahora = Instant.now();
        inventario.setStockFisico(stockResultante);
        inventario.setFechaActualizacion(ahora);
        inventarioRepository.save(inventario);

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setSede(sede);
        movimiento.setProducto(producto);
        movimiento.setUsuario(usuario);
        movimiento.setUnidadMedida(unidad);
        movimiento.setTipoMovimiento(TipoMovimientoInventario.ANULACION_VENTA);
        movimiento.setCantidad(cantidadNormalizada);
        movimiento.setCantidadBase(cantidadBaseNormalizada);
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockResultante(stockResultante);
        movimiento.setDocumentoOrigen("VENTA");
        movimiento.setIdOrigen(idVenta);
        movimiento.setMotivo("Reposición por anulación de la venta #" + idVenta);
        movimiento.setFechaHora(ahora);
        return movimientoRepository.save(movimiento);
    }

    @Transactional
    public MovimientoInventario registrarDevolucionVenta(
        Sede sede,
        Producto producto,
        UnidadMedida unidad,
        BigDecimal cantidad,
        BigDecimal cantidadBase,
        Usuario usuario,
        Long idDevolucion,
        Long idVenta
    ) {
        Inventario inventario = inventarioRepository
            .findForUpdate(sede.getId(), producto.getId())
            .orElseGet(() -> crearInventarioVacio(sede, producto));
        BigDecimal cantidadNormalizada = cantidad.setScale(
            ESCALA_STOCK,
            RoundingMode.UNNECESSARY
        );
        BigDecimal cantidadBaseNormalizada = cantidadBase.setScale(
            ESCALA_STOCK,
            RoundingMode.UNNECESSARY
        );
        BigDecimal stockAnterior = inventario.getStockFisico();
        BigDecimal stockResultante = stockAnterior.add(cantidadBaseNormalizada);
        validarCapacidadStock(stockResultante);

        Instant ahora = Instant.now();
        inventario.setStockFisico(stockResultante);
        inventario.setFechaActualizacion(ahora);
        inventarioRepository.save(inventario);

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setSede(sede);
        movimiento.setProducto(producto);
        movimiento.setUsuario(usuario);
        movimiento.setUnidadMedida(unidad);
        movimiento.setTipoMovimiento(TipoMovimientoInventario.DEVOLUCION_ENTRADA);
        movimiento.setCantidad(cantidadNormalizada);
        movimiento.setCantidadBase(cantidadBaseNormalizada);
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockResultante(stockResultante);
        movimiento.setDocumentoOrigen("DEVOLUCION");
        movimiento.setIdOrigen(idDevolucion);
        movimiento.setMotivo("Producto apto devuelto de la venta #" + idVenta);
        movimiento.setFechaHora(ahora);
        return movimientoRepository.save(movimiento);
    }

    @Transactional
    public MovimientoInventario registrarSalidaCambio(
        Sede sede,
        Producto producto,
        UnidadMedida unidad,
        BigDecimal cantidad,
        BigDecimal cantidadBase,
        Usuario usuario,
        Long idDevolucion
    ) {
        Inventario inventario = inventarioRepository
            .findForUpdate(sede.getId(), producto.getId())
            .orElseThrow(() -> new ReglaNegocioException(
                "Stock insuficiente para " + producto.getCodigoInterno()
                    + ". Disponible: 0.000"
            ));
        BigDecimal cantidadNormalizada = cantidad.setScale(
            ESCALA_STOCK,
            RoundingMode.UNNECESSARY
        );
        BigDecimal cantidadBaseNormalizada = cantidadBase.setScale(
            ESCALA_STOCK,
            RoundingMode.UNNECESSARY
        );
        if (cantidadBaseNormalizada.compareTo(inventario.getStockFisico()) > 0) {
            throw new ReglaNegocioException(
                "Stock físico insuficiente para " + producto.getCodigoInterno()
                    + ". Físico: " + inventario.getStockFisico().toPlainString()
            );
        }
        BigDecimal disponible = inventario.getStockDisponible();
        if (cantidadBaseNormalizada.compareTo(disponible) > 0) {
            throw new ReglaNegocioException(
                "Stock insuficiente para " + producto.getCodigoInterno()
                    + ". Disponible: " + disponible.toPlainString()
            );
        }
        consumirSueltoYPresentacionesAbiertas(
            sede, producto, cantidadBaseNormalizada, inventario
        );

        Instant ahora = Instant.now();
        BigDecimal stockAnterior = inventario.getStockFisico();
        BigDecimal stockResultante = stockAnterior.subtract(cantidadBaseNormalizada);
        inventario.setStockFisico(stockResultante);
        inventario.setFechaActualizacion(ahora);
        inventarioRepository.save(inventario);

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setSede(sede);
        movimiento.setProducto(producto);
        movimiento.setUsuario(usuario);
        movimiento.setUnidadMedida(unidad);
        movimiento.setTipoMovimiento(TipoMovimientoInventario.DEVOLUCION_SALIDA);
        movimiento.setCantidad(cantidadNormalizada.negate());
        movimiento.setCantidadBase(cantidadBaseNormalizada.negate());
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockResultante(stockResultante);
        movimiento.setDocumentoOrigen("CAMBIO");
        movimiento.setIdOrigen(idDevolucion);
        movimiento.setMotivo(
            "Producto de reemplazo entregado por la devolución #" + idDevolucion
        );
        movimiento.setFechaHora(ahora);
        return movimientoRepository.save(movimiento);
    }

    private MovimientoInventario registrarSalidaVenta(
        Sede sede,
        Producto producto,
        UnidadMedida unidad,
        BigDecimal cantidad,
        BigDecimal cantidadBase,
        Usuario usuario,
        Long idVenta,
        Long idPedido
    ) {
        Inventario inventario = inventarioRepository
            .findForUpdate(sede.getId(), producto.getId())
            .orElseThrow(() -> new ReglaNegocioException(
                "Stock insuficiente para " + producto.getCodigoInterno()
                    + ". Disponible: 0.000"
            ));
        BigDecimal cantidadNormalizada = cantidad.setScale(
            ESCALA_STOCK,
            RoundingMode.UNNECESSARY
        );
        BigDecimal cantidadBaseNormalizada = cantidadBase.setScale(
            ESCALA_STOCK,
            RoundingMode.UNNECESSARY
        );
        if (cantidadBaseNormalizada.compareTo(inventario.getStockFisico()) > 0) {
            throw new ReglaNegocioException(
                "Stock físico insuficiente para " + producto.getCodigoInterno()
                    + ". Físico: " + inventario.getStockFisico().toPlainString()
            );
        }
        if (idPedido == null) {
            BigDecimal disponible = inventario.getStockDisponible();
            if (cantidadBaseNormalizada.compareTo(disponible) > 0) {
                throw new ReglaNegocioException(
                    "Stock insuficiente para " + producto.getCodigoInterno()
                        + ". Disponible: " + disponible.toPlainString()
                );
            }
        } else if (cantidadBaseNormalizada.compareTo(
            inventario.getStockReservado()
        ) > 0) {
            throw new OperacionNoPermitidaException(
                "La reserva del pedido supera el stock reservado del producto "
                    + producto.getCodigoInterno()
            );
        }

        Instant ahora = Instant.now();
        BigDecimal stockAnterior = inventario.getStockFisico();
        BigDecimal stockResultante = stockAnterior.subtract(cantidadBaseNormalizada);
        inventario.setStockFisico(stockResultante);
        if (idPedido != null) {
            inventario.setStockReservado(
                inventario.getStockReservado().subtract(cantidadBaseNormalizada)
            );
        }
        inventario.setFechaActualizacion(ahora);
        inventarioRepository.save(inventario);

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setSede(sede);
        movimiento.setProducto(producto);
        movimiento.setUsuario(usuario);
        movimiento.setUnidadMedida(unidad);
        movimiento.setTipoMovimiento(TipoMovimientoInventario.VENTA);
        movimiento.setCantidad(cantidadNormalizada.negate());
        movimiento.setCantidadBase(cantidadBaseNormalizada.negate());
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockResultante(stockResultante);
        movimiento.setDocumentoOrigen("VENTA");
        movimiento.setIdOrigen(idVenta);
        movimiento.setMotivo(idPedido == null
            ? "Salida confirmada por la venta #" + idVenta
            : "Reserva del pedido #" + idPedido + " consumida por la venta #" + idVenta);
        movimiento.setFechaHora(ahora);
        return movimientoRepository.save(movimiento);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<MovimientoInventarioResponse> listarMovimientos(
        Long idSede,
        Long idProducto,
        TipoMovimientoInventario tipo,
        LocalDate desde,
        LocalDate hasta,
        Pageable pageable
    ) {
        Sede sede = resolverSede(idSede);
        RangoFechas rango = convertirRango(desde, hasta);
        Long filtroProducto = idProducto == null ? 0L : idProducto;
        Page<MovimientoInventario> movimientos = tipo == null
            ? movimientoRepository.buscar(
                sede.getId(),
                filtroProducto,
                rango.desde(),
                rango.hastaExclusiva(),
                pageable
            )
            : movimientoRepository.buscarPorTipo(
                sede.getId(),
                filtroProducto,
                tipo,
                rango.desde(),
                rango.hastaExclusiva(),
                pageable
            );
        Page<MovimientoInventarioResponse> pagina = movimientos
            .map(MovimientoInventarioResponse::from);
        return PaginaResponse.from(pagina);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<MovimientoInventarioResponse> consultarKardex(
        Long idProducto,
        Long idSede,
        TipoMovimientoInventario tipo,
        LocalDate desde,
        LocalDate hasta,
        Pageable pageable
    ) {
        buscarProducto(idProducto, false);
        return listarMovimientos(idSede, idProducto, tipo, desde, hasta, pageable);
    }

    private PaginaResponse<StockInventarioResponse> mapearPaginaStock(
        Sede sede,
        Page<Producto> productos
    ) {
        Map<Long, Inventario> inventarios = productos.isEmpty()
            ? Map.of()
            : inventarioRepository.findAllBySedeIdAndProductoIdIn(
                sede.getId(),
                productos.getContent().stream().map(Producto::getId).toList()
            ).stream().collect(Collectors.toMap(
                inventario -> inventario.getProducto().getId(),
                Function.identity()
            ));

        return PaginaResponse.from(productos.map(producto ->
            StockInventarioResponse.from(sede, producto, inventarios.get(producto.getId()))
        ));
    }

    private Sede resolverSede(Long idSede) {
        Sede sede = idSede == null
            ? sedeRepository.findFirstByEstadoIgnoreCaseOrderByIdAsc("ACTIVO")
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una sede activa"))
            : sedeRepository.findById(idSede)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la sede solicitada"));
        if (!"ACTIVO".equalsIgnoreCase(sede.getEstado())) {
            throw new OperacionNoPermitidaException("No se puede operar con una sede inactiva");
        }
        return sede;
    }

    private Producto buscarProducto(Long idProducto, boolean exigirActivo) {
        Producto producto = productoRepository.findByIdWithReferencias(idProducto)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto solicitado"));
        if (exigirActivo && producto.getEstado() != EstadoCatalogo.ACTIVO) {
            throw new OperacionNoPermitidaException("No se puede ajustar un producto inactivo");
        }
        return producto;
    }

    private UnidadMedida buscarUnidad(Long idUnidad) {
        UnidadMedida unidad = unidadMedidaRepository.findById(idUnidad)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la unidad de medida solicitada"
            ));
        if (unidad.getEstado() != EstadoCatalogo.ACTIVO) {
            throw new OperacionNoPermitidaException("No se puede utilizar una unidad inactiva");
        }
        return unidad;
    }

    private Usuario buscarUsuarioActivo(String usuarioLogin) {
        Usuario usuario = usuarioRepository.findByUsuarioLoginIgnoreCase(usuarioLogin)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe el usuario autenticado"
            ));
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new OperacionNoPermitidaException("El usuario autenticado no está activo");
        }
        return usuario;
    }

    private void validarCantidadPermitida(UnidadMedida unidad, BigDecimal cantidad) {
        if (!unidad.isPermiteDecimales() && cantidad.stripTrailingZeros().scale() > 0) {
            throw new SolicitudInvalidaException(
                "La unidad " + unidad.getCodigo() + " no permite cantidades decimales"
            );
        }
    }

    private BigDecimal convertirAUnidadBase(
        Producto producto,
        UnidadMedida unidad,
        BigDecimal cantidad
    ) {
        Long idUnidadBase = producto.getUnidadBase().getId();
        if (idUnidadBase.equals(unidad.getId())) {
            return cantidad;
        }

        return conversionRepository
            .findByProductoIdAndUnidadOrigenIdAndUnidadDestinoIdAndEstado(
                producto.getId(),
                unidad.getId(),
                idUnidadBase,
                EstadoCatalogo.ACTIVO
            )
            .map(conversion -> cantidad.multiply(conversion.getFactorConversion()))
            .orElseGet(() -> conversionRepository
                .findByProductoIdAndUnidadOrigenIdAndUnidadDestinoIdAndEstado(
                    producto.getId(),
                    idUnidadBase,
                    unidad.getId(),
                    EstadoCatalogo.ACTIVO
                )
                .map(conversion -> dividir(cantidad, conversion))
                .orElseThrow(() -> new OperacionNoPermitidaException(
                    "No existe una conversión activa entre la unidad indicada y la unidad base"
                )))
            .setScale(ESCALA_STOCK, RoundingMode.HALF_UP);
    }

    private BigDecimal dividir(
        BigDecimal cantidad,
        ProductoUnidadConversion conversion
    ) {
        return cantidad.divide(conversion.getFactorConversion(), 9, RoundingMode.HALF_UP);
    }

    private Inventario crearInventarioVacio(Sede sede, Producto producto) {
        Inventario inventario = new Inventario();
        inventario.setSede(sede);
        inventario.setProducto(producto);
        inventario.setStockFisico(BigDecimal.ZERO.setScale(ESCALA_STOCK));
        inventario.setStockReservado(BigDecimal.ZERO.setScale(ESCALA_STOCK));
        inventario.setStockMinimo(producto.getStockMinimo());
        inventario.setFechaActualizacion(Instant.now());
        return inventarioRepository.save(inventario);
    }

    private BigDecimal calcularStockNoVinculado(
        Sede sede,
        Producto producto,
        Inventario inventario
    ) {
        BigDecimal stockRastreado = existenciaPresentacionRepository
            .findAllForUpdate(
                sede.getId(),
                producto.getId(),
                EnumSet.of(
                    EstadoExistenciaPresentacion.CERRADO,
                    EstadoExistenciaPresentacion.ABIERTO
                )
            ).stream()
            .map(ExistenciaPresentacion::getCantidadDisponibleBase)
            .reduce(BigDecimal.ZERO.setScale(ESCALA_STOCK), BigDecimal::add);
        return inventario.getStockFisico()
            .subtract(stockRastreado)
            .max(BigDecimal.ZERO.setScale(ESCALA_STOCK));
    }

    private MovimientoInventario registrarConversionBultos(
        Sede sede,
        Producto producto,
        PresentacionProducto presentacion,
        Usuario usuario,
        int cantidadBultos,
        BigDecimal cantidadBase,
        BigDecimal stockActual,
        String motivo,
        Instant fechaHora
    ) {
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setSede(sede);
        movimiento.setProducto(producto);
        movimiento.setUsuario(usuario);
        movimiento.setUnidadMedida(presentacion.getUnidadMedida());
        movimiento.setTipoMovimiento(TipoMovimientoInventario.CONVERSION_BULTOS);
        movimiento.setCantidad(
            BigDecimal.valueOf(cantidadBultos).setScale(ESCALA_STOCK)
        );
        movimiento.setCantidadBase(cantidadBase);
        movimiento.setStockAnterior(stockActual);
        movimiento.setStockResultante(stockActual);
        movimiento.setDocumentoOrigen("CONVERSION_BULTOS");
        movimiento.setMotivo(motivo);
        movimiento.setFechaHora(fechaHora);
        return movimientoRepository.save(movimiento);
    }

    private EntradaPresentacionesResultado registrarEntradaPresentaciones(
        Sede sede,
        Producto producto,
        PresentacionProducto presentacion,
        List<BigDecimal> contenidos,
        Usuario usuario,
        RecepcionCompra recepcion,
        Long idCompra,
        String motivo,
        TipoMovimientoInventario tipoMovimiento
    ) {
        BigDecimal cantidadBase = contenidos.stream()
            .reduce(BigDecimal.ZERO.setScale(ESCALA_STOCK), BigDecimal::add);
        Inventario inventario = inventarioRepository
            .findForUpdate(sede.getId(), producto.getId())
            .orElseGet(() -> crearInventarioVacio(sede, producto));
        BigDecimal stockAnterior = inventario.getStockFisico();
        BigDecimal stockResultante = stockAnterior.add(cantidadBase);
        validarCapacidadStock(stockResultante);
        Instant ahora = Instant.now();
        inventario.setStockFisico(stockResultante);
        inventario.setFechaActualizacion(ahora);
        inventarioRepository.save(inventario);

        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setSede(sede);
        movimiento.setProducto(producto);
        movimiento.setUsuario(usuario);
        movimiento.setUnidadMedida(presentacion.getUnidadMedida());
        movimiento.setTipoMovimiento(tipoMovimiento);
        movimiento.setCantidad(BigDecimal.valueOf(contenidos.size()).setScale(ESCALA_STOCK));
        movimiento.setCantidadBase(cantidadBase);
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockResultante(stockResultante);
        movimiento.setDocumentoOrigen(recepcion == null ? "INGRESO_PRESENTACIONES" : "RECEPCION_COMPRA");
        movimiento.setIdOrigen(recepcion == null ? null : recepcion.getId());
        movimiento.setMotivo(motivo);
        movimiento.setFechaHora(ahora);
        movimiento = movimientoRepository.save(movimiento);

        List<ExistenciaPresentacion> existencias = crearExistenciasPresentacion(
            sede, presentacion, contenidos, recepcion, ahora
        );
        return new EntradaPresentacionesResultado(existencias, movimiento, inventario);
    }

    private List<ExistenciaPresentacion> crearExistenciasPresentacion(
        Sede sede,
        PresentacionProducto presentacion,
        List<BigDecimal> contenidos,
        RecepcionCompra recepcion,
        Instant fechaIngreso
    ) {
        List<ExistenciaPresentacion> existencias = contenidos.stream().map(contenido -> {
            ExistenciaPresentacion existencia = new ExistenciaPresentacion();
            existencia.setPresentacion(presentacion);
            existencia.setSede(sede);
            existencia.setRecepcionCompra(recepcion);
            existencia.setCantidadInicialBase(contenido);
            existencia.setCantidadDisponibleBase(contenido);
            existencia.setEstado(EstadoExistenciaPresentacion.CERRADO);
            existencia.setFechaIngreso(fechaIngreso);
            return existenciaPresentacionRepository.saveAndFlush(existencia);
        }).toList();
        existencias.forEach(existencia ->
            existencia.setCodigo("BUL-" + String.format("%08d", existencia.getId()))
        );
        return existencias;
    }

    private List<BigDecimal> normalizarContenidos(
        Producto producto,
        PresentacionProducto presentacion,
        List<BigDecimal> contenidosSolicitados
    ) {
        if (presentacion.getEstado() != EstadoCatalogo.ACTIVO
            || !presentacion.getProducto().getId().equals(producto.getId())) {
            throw new OperacionNoPermitidaException(
                "La presentación no está activa para el producto"
            );
        }
        if (contenidosSolicitados == null || contenidosSolicitados.isEmpty()) {
            throw new SolicitudInvalidaException(
                "Debe informar el contenido de cada caja, paquete o rollo"
            );
        }
        return contenidosSolicitados.stream().map(valor -> {
            if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
                throw new SolicitudInvalidaException(
                    "Cada presentación debe tener un contenido mayor que cero"
                );
            }
            BigDecimal contenido = valor.setScale(ESCALA_STOCK, RoundingMode.UNNECESSARY);
            if (!producto.getUnidadBase().isPermiteDecimales()
                && contenido.stripTrailingZeros().scale() > 0) {
                throw new SolicitudInvalidaException(
                    "La unidad base " + producto.getUnidadBase().getCodigo()
                        + " no admite contenidos decimales"
                );
            }
            if (!presentacion.isContenidoVariable()
                && contenido.compareTo(presentacion.getContenidoBasePredeterminado()) != 0) {
                throw new SolicitudInvalidaException(
                    "La presentación " + presentacion.getNombre() + " debe contener "
                        + presentacion.getContenidoBasePredeterminado().toPlainString() + " "
                        + producto.getUnidadBase().getCodigo()
                );
            }
            return contenido;
        }).toList();
    }

    private List<BigDecimal> resolverContenidosIngreso(
        Producto producto,
        PresentacionProducto presentacion,
        Integer cantidadBultos,
        List<BigDecimal> contenidosSolicitados
    ) {
        if (presentacion.isContenidoVariable()) {
            if (contenidosSolicitados == null || contenidosSolicitados.isEmpty()) {
                throw new SolicitudInvalidaException(
                    "Informe el contenido real de cada " + presentacion.getNombre()
                );
            }
            if (cantidadBultos != null && cantidadBultos != contenidosSolicitados.size()) {
                throw new SolicitudInvalidaException(
                    "La cantidad de bultos no coincide con los contenidos informados"
                );
            }
            return normalizarContenidos(producto, presentacion, contenidosSolicitados);
        }

        int cantidad = cantidadBultos == null
            ? contenidosSolicitados == null ? 0 : contenidosSolicitados.size()
            : cantidadBultos;
        if (cantidad < 1 || cantidad > 200) {
            throw new SolicitudInvalidaException(
                "Debe registrar entre 1 y 200 bultos por operación"
            );
        }
        if (contenidosSolicitados != null
            && !contenidosSolicitados.isEmpty()
            && contenidosSolicitados.size() != cantidad) {
            throw new SolicitudInvalidaException(
                "La cantidad de bultos no coincide con los contenidos informados"
            );
        }
        return normalizarContenidos(
            producto,
            presentacion,
            java.util.Collections.nCopies(
                cantidad,
                presentacion.getContenidoBasePredeterminado()
            )
        );
    }

    private PresentacionProducto buscarPresentacionActiva(Long id, Long idProducto) {
        PresentacionProducto presentacion = presentacionProductoRepository.findByIdAndProductoId(
            id, idProducto
        ).orElseThrow(() -> new RecursoNoEncontradoException(
            "No existe la presentación solicitada para el producto"
        ));
        if (presentacion.getEstado() != EstadoCatalogo.ACTIVO) {
            throw new OperacionNoPermitidaException("La presentación se encuentra inactiva");
        }
        return presentacion;
    }

    private void agotar(ExistenciaPresentacion existencia) {
        existencia.setCantidadDisponibleBase(BigDecimal.ZERO.setScale(ESCALA_STOCK));
        existencia.setEstado(EstadoExistenciaPresentacion.AGOTADO);
        existencia.setFechaAgotamiento(Instant.now());
    }

    private void registrarConsumo(
        DetalleVenta detalle,
        ExistenciaPresentacion existencia,
        BigDecimal cantidad
    ) {
        ConsumoExistenciaPresentacion consumo = new ConsumoExistenciaPresentacion();
        consumo.setDetalleVenta(detalle);
        consumo.setExistencia(existencia);
        consumo.setCantidadBase(cantidad);
        consumoPresentacionRepository.save(consumo);
    }

    private void consumirPresentacionesFraccionadas(
        Sede sede,
        DetalleVenta detalle,
        BigDecimal requerida,
        BigDecimal stockUtilizable
    ) {
        List<ExistenciaPresentacion> rastreadas = existenciaPresentacionRepository
            .findAllForUpdate(
                sede.getId(), detalle.getProducto().getId(),
                EnumSet.of(
                    EstadoExistenciaPresentacion.CERRADO,
                    EstadoExistenciaPresentacion.ABIERTO
                )
            );
        BigDecimal totalRastreado = rastreadas.stream()
            .map(ExistenciaPresentacion::getCantidadDisponibleBase)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal suelto = stockUtilizable.subtract(totalRastreado).max(BigDecimal.ZERO);
        BigDecimal porConsumir = requerida.subtract(suelto).max(BigDecimal.ZERO);
        for (ExistenciaPresentacion existencia : rastreadas) {
            if (porConsumir.signum() == 0) break;
            if (existencia.getEstado() != EstadoExistenciaPresentacion.ABIERTO) continue;
            BigDecimal consumo = porConsumir.min(existencia.getCantidadDisponibleBase());
            existencia.setCantidadDisponibleBase(
                existencia.getCantidadDisponibleBase().subtract(consumo)
            );
            if (existencia.getCantidadDisponibleBase().signum() == 0) {
                agotar(existencia);
            }
            registrarConsumo(detalle, existencia, consumo);
            porConsumir = porConsumir.subtract(consumo);
        }
        if (porConsumir.signum() > 0) {
            throw new ReglaNegocioException(
                "Debe abrir una caja, paquete o rollo para disponer de "
                    + porConsumir.toPlainString() + " "
                    + detalle.getProducto().getUnidadBase().getCodigo()
            );
        }
    }

    private void consumirSueltoYPresentacionesAbiertas(
        Sede sede,
        Producto producto,
        BigDecimal cantidadBase,
        Inventario inventario
    ) {
        List<ExistenciaPresentacion> rastreadas = existenciaPresentacionRepository
            .findAllForUpdate(
                sede.getId(), producto.getId(),
                EnumSet.of(
                    EstadoExistenciaPresentacion.CERRADO,
                    EstadoExistenciaPresentacion.ABIERTO
                )
            );
        BigDecimal totalRastreado = rastreadas.stream()
            .map(ExistenciaPresentacion::getCantidadDisponibleBase)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal suelto = inventario.getStockDisponible().subtract(totalRastreado)
            .max(BigDecimal.ZERO);
        BigDecimal porConsumir = cantidadBase.subtract(suelto).max(BigDecimal.ZERO);
        for (ExistenciaPresentacion existencia : rastreadas) {
            if (porConsumir.signum() == 0) break;
            if (existencia.getEstado() != EstadoExistenciaPresentacion.ABIERTO) continue;
            BigDecimal consumo = porConsumir.min(existencia.getCantidadDisponibleBase());
            existencia.setCantidadDisponibleBase(
                existencia.getCantidadDisponibleBase().subtract(consumo)
            );
            if (existencia.getCantidadDisponibleBase().signum() == 0) {
                agotar(existencia);
            }
            porConsumir = porConsumir.subtract(consumo);
        }
        if (porConsumir.signum() > 0) {
            throw new ReglaNegocioException(
                "Debe abrir una caja, paquete o rollo para disponer de "
                    + porConsumir.toPlainString() + " "
                    + producto.getUnidadBase().getCodigo()
            );
        }
    }

    private MovimientoInventario crearMovimientoTransferencia(
        Sede sede,
        Producto producto,
        UnidadMedida unidad,
        Usuario usuario,
        TipoMovimientoInventario tipo,
        BigDecimal cantidad,
        BigDecimal cantidadBase,
        BigDecimal stockAnterior,
        BigDecimal stockResultante,
        Long idTransferencia,
        String motivo,
        Instant fechaHora
    ) {
        MovimientoInventario movimiento = new MovimientoInventario();
        movimiento.setSede(sede);
        movimiento.setProducto(producto);
        movimiento.setUsuario(usuario);
        movimiento.setUnidadMedida(unidad);
        movimiento.setTipoMovimiento(tipo);
        movimiento.setCantidad(cantidad);
        movimiento.setCantidadBase(cantidadBase);
        movimiento.setStockAnterior(stockAnterior);
        movimiento.setStockResultante(stockResultante);
        movimiento.setDocumentoOrigen("TRANSFERENCIA");
        movimiento.setIdOrigen(idTransferencia);
        movimiento.setMotivo(motivo);
        movimiento.setFechaHora(fechaHora);
        return movimientoRepository.save(movimiento);
    }

    private BigDecimal firmar(TipoAjusteInventario tipo, BigDecimal cantidad) {
        return tipo == TipoAjusteInventario.ENTRADA ? cantidad : cantidad.negate();
    }

    private void validarCapacidadStock(BigDecimal stock) {
        BigDecimal normalizado = stock.setScale(ESCALA_STOCK, RoundingMode.UNNECESSARY);
        int enteros = Math.max(0, normalizado.precision() - normalizado.scale());
        if (enteros > 11) {
            throw new SolicitudInvalidaException(
                "El stock resultante supera la capacidad máxima permitida"
            );
        }
    }

    private TipoMovimientoInventario tipoMovimiento(TipoAjusteInventario tipo) {
        return tipo == TipoAjusteInventario.ENTRADA
            ? TipoMovimientoInventario.AJUSTE_ENTRADA
            : TipoMovimientoInventario.AJUSTE_SALIDA;
    }

    private RangoFechas convertirRango(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new SolicitudInvalidaException(
                "La fecha inicial no puede ser posterior a la fecha final"
            );
        }
        Instant inicio = desde == null
            ? INICIO_CONSULTAS
            : desde.atStartOfDay(ZONA_NEGOCIO).toInstant();
        Instant finExclusivo = hasta == null
            ? FIN_CONSULTAS
            : hasta.plusDays(1).atStartOfDay(ZONA_NEGOCIO).toInstant();
        return new RangoFechas(inicio, finExclusivo);
    }

    private String normalizarBusqueda(String buscar) {
        return buscar == null ? "" : buscar.strip();
    }

    private record RangoFechas(Instant desde, Instant hastaExclusiva) {
    }

    private record EntradaPresentacionesResultado(
        List<ExistenciaPresentacion> presentaciones,
        MovimientoInventario movimiento,
        Inventario inventario
    ) {
    }
}
