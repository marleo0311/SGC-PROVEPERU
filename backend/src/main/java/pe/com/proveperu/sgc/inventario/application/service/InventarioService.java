package pe.com.proveperu.sgc.inventario.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.catalogo.domain.model.Producto;
import pe.com.proveperu.sgc.catalogo.domain.model.ProductoUnidadConversion;
import pe.com.proveperu.sgc.catalogo.domain.model.UnidadMedida;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.ProductoRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.ProductoUnidadConversionRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.UnidadMedidaRepository;
import pe.com.proveperu.sgc.inventario.api.dto.AjusteInventarioRequest;
import pe.com.proveperu.sgc.inventario.api.dto.AjusteInventarioResponse;
import pe.com.proveperu.sgc.inventario.api.dto.MovimientoInventarioResponse;
import pe.com.proveperu.sgc.inventario.api.dto.StockInventarioResponse;
import pe.com.proveperu.sgc.inventario.api.dto.StockMinimoInventarioRequest;
import pe.com.proveperu.sgc.inventario.api.dto.TransferenciaInventarioRequest;
import pe.com.proveperu.sgc.inventario.api.dto.TransferenciaInventarioResponse;
import pe.com.proveperu.sgc.inventario.domain.model.Inventario;
import pe.com.proveperu.sgc.inventario.domain.model.MovimientoInventario;
import pe.com.proveperu.sgc.inventario.domain.model.Sede;
import pe.com.proveperu.sgc.inventario.domain.model.TipoAjusteInventario;
import pe.com.proveperu.sgc.inventario.domain.model.TipoMovimientoInventario;
import pe.com.proveperu.sgc.inventario.domain.model.TransferenciaInventario;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.InventarioRepository;
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
    private final InventarioRepository inventarioRepository;
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
}
