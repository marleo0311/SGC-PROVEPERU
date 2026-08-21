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
import pe.com.proveperu.sgc.inventario.domain.model.Inventario;
import pe.com.proveperu.sgc.inventario.domain.model.MovimientoInventario;
import pe.com.proveperu.sgc.inventario.domain.model.Sede;
import pe.com.proveperu.sgc.inventario.domain.model.TipoAjusteInventario;
import pe.com.proveperu.sgc.inventario.domain.model.TipoMovimientoInventario;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.InventarioRepository;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.MovimientoInventarioRepository;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.SedeRepository;
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
        LocalDate desde,
        LocalDate hasta,
        Pageable pageable
    ) {
        buscarProducto(idProducto, false);
        return listarMovimientos(idSede, idProducto, null, desde, hasta, pageable);
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
        inventario.setFechaActualizacion(Instant.now());
        return inventarioRepository.save(inventario);
    }

    private BigDecimal firmar(TipoAjusteInventario tipo, BigDecimal cantidad) {
        return tipo == TipoAjusteInventario.ENTRADA ? cantidad : cantidad.negate();
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
