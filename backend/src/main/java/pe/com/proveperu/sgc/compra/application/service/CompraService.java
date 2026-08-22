package pe.com.proveperu.sgc.compra.application.service;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
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
import pe.com.proveperu.sgc.catalogo.domain.model.Producto;
import pe.com.proveperu.sgc.catalogo.domain.model.UnidadMedida;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.ProductoRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.ProductoUnidadConversionRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.UnidadMedidaRepository;
import pe.com.proveperu.sgc.compra.api.dto.CompraDetalleRequest;
import pe.com.proveperu.sgc.compra.api.dto.CompraGuardarRequest;
import pe.com.proveperu.sgc.compra.api.dto.CompraResponse;
import pe.com.proveperu.sgc.compra.api.dto.CompraResumenResponse;
import pe.com.proveperu.sgc.compra.domain.model.Compra;
import pe.com.proveperu.sgc.compra.domain.model.DetalleCompra;
import pe.com.proveperu.sgc.compra.domain.model.EstadoCompra;
import pe.com.proveperu.sgc.compra.infrastructure.persistence.CompraRepository;
import pe.com.proveperu.sgc.proveedor.domain.model.Proveedor;
import pe.com.proveperu.sgc.proveedor.infrastructure.persistence.ProveedorRepository;
import pe.com.proveperu.sgc.security.application.exception.ConflictoNegocioException;
import pe.com.proveperu.sgc.security.application.exception.OperacionNoPermitidaException;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;
import pe.com.proveperu.sgc.security.domain.model.EstadoUsuario;
import pe.com.proveperu.sgc.security.domain.model.Usuario;
import pe.com.proveperu.sgc.security.infrastructure.persistence.UsuarioRepository;
import pe.com.proveperu.sgc.shared.api.dto.PaginaResponse;
import pe.com.proveperu.sgc.shared.application.exception.SolicitudInvalidaException;
import pe.com.proveperu.sgc.transporte.api.dto.GastoCrearRequest;
import pe.com.proveperu.sgc.transporte.api.dto.GastoResponse;
import pe.com.proveperu.sgc.transporte.application.service.GastoService;

@Service
@RequiredArgsConstructor
public class CompraService {

    private static final int ESCALA_DINERO = 2;
    private static final int ESCALA_CANTIDAD = 3;
    private static final int MAX_ENTEROS_DINERO = 12;

    private final CompraRepository compraRepository;
    private final ProveedorRepository proveedorRepository;
    private final ProductoRepository productoRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;
    private final ProductoUnidadConversionRepository conversionRepository;
    private final UsuarioRepository usuarioRepository;
    private final GastoService gastoService;

    @Transactional(readOnly = true)
    public PaginaResponse<CompraResumenResponse> listar(
        Long idProveedor,
        EstadoCompra estado,
        LocalDate desde,
        LocalDate hasta,
        Pageable pageable
    ) {
        validarRango(desde, hasta);
        Page<CompraResumenResponse> pagina = compraRepository.findAll(
            crearFiltros(idProveedor, estado, desde, hasta),
            pageable
        ).map(CompraResumenResponse::from);
        return PaginaResponse.from(pagina);
    }

    @Transactional(readOnly = true)
    public CompraResponse obtener(Long id) {
        return CompraResponse.from(buscarCompraDetallada(id));
    }

    @Transactional
    public CompraResponse crear(CompraGuardarRequest request, String usuarioLogin) {
        Proveedor proveedor = buscarProveedorActivo(request.idProveedor());
        Usuario usuario = buscarUsuarioActivo(usuarioLogin);
        String tipoComprobante = normalizarMayusculas(request.tipoComprobante());
        String numeroComprobante = normalizarMayusculas(request.numeroComprobante());
        validarComprobanteDisponible(
            proveedor.getId(),
            tipoComprobante,
            numeroComprobante,
            null
        );

        Compra compra = new Compra();
        compra.setProveedor(proveedor);
        compra.setUsuario(usuario);
        compra.setEstado(EstadoCompra.REGISTRADA);
        compra.setGastosAdicionales(dinero(BigDecimal.ZERO));
        aplicarDatos(compra, request, tipoComprobante, numeroComprobante);
        return CompraResponse.from(compraRepository.saveAndFlush(compra));
    }

    @Transactional
    public CompraResponse actualizar(Long id, CompraGuardarRequest request) {
        Compra compra = buscarCompraDetallada(id);
        validarEditable(compra);
        Proveedor proveedor = buscarProveedorActivo(request.idProveedor());
        String tipoComprobante = normalizarMayusculas(request.tipoComprobante());
        String numeroComprobante = normalizarMayusculas(request.numeroComprobante());
        validarComprobanteDisponible(
            proveedor.getId(),
            tipoComprobante,
            numeroComprobante,
            id
        );

        compra.getDetalles().clear();
        compraRepository.flush();
        compra.setProveedor(proveedor);
        aplicarDatos(compra, request, tipoComprobante, numeroComprobante);
        return CompraResponse.from(compraRepository.saveAndFlush(compra));
    }

    @Transactional
    public CompraResponse cambiarEstado(Long id, EstadoCompra nuevoEstado) {
        Compra compra = buscarCompraDetallada(id);
        if (nuevoEstado != EstadoCompra.ANULADA) {
            throw new OperacionNoPermitidaException(
                "Desde este endpoint solo se puede anular una compra"
            );
        }
        if (compra.getEstado() == EstadoCompra.ANULADA) {
            return CompraResponse.from(compra);
        }
        validarEditable(compra);
        compra.setEstado(EstadoCompra.ANULADA);
        return CompraResponse.from(compraRepository.saveAndFlush(compra));
    }

    @Transactional
    public GastoResponse crearGasto(
        Long idCompra,
        GastoCrearRequest request,
        String usuarioLogin
    ) {
        Compra compra = buscarCompraDetallada(idCompra);
        if (compra.getEstado() == EstadoCompra.ANULADA
            || compra.getEstado() == EstadoCompra.RECIBIDA) {
            throw new OperacionNoPermitidaException(
                "No se pueden agregar gastos a una compra " + compra.getEstado()
            );
        }
        GastoResponse gasto = gastoService.crearParaCompra(
            idCompra,
            request,
            usuarioLogin
        );
        BigDecimal gastos = dinero(gastoService.sumarImportesPorCompra(idCompra));
        compra.setGastosAdicionales(gastos);
        compra.setTotal(calcularTotal(compra.getSubtotal(), compra.getIgv(), gastos));
        compraRepository.saveAndFlush(compra);
        return gasto;
    }

    @Transactional(readOnly = true)
    public List<GastoResponse> listarGastos(Long idCompra) {
        buscarCompraDetallada(idCompra);
        return gastoService.listarPorCompra(idCompra);
    }

    private void aplicarDatos(
        Compra compra,
        CompraGuardarRequest request,
        String tipoComprobante,
        String numeroComprobante
    ) {
        compra.setFecha(request.fecha());
        compra.setTipoComprobante(tipoComprobante);
        compra.setNumeroComprobante(numeroComprobante);
        compra.setCondicionPago(request.condicionPago());
        compra.setIgv(validarDinero(request.igv(), "El IGV"));

        Set<DetalleClave> detallesUnicos = new HashSet<>();
        BigDecimal subtotalCompra = dinero(BigDecimal.ZERO);
        for (CompraDetalleRequest item : request.detalles()) {
            DetalleClave clave = new DetalleClave(item.idProducto(), item.idUnidadMedida());
            if (!detallesUnicos.add(clave)) {
                throw new SolicitudInvalidaException(
                    "No se puede repetir el mismo producto y unidad en la compra"
                );
            }
            DetalleCompra detalle = crearDetalle(item);
            compra.agregarDetalle(detalle);
            subtotalCompra = subtotalCompra.add(detalle.getSubtotal());
        }
        subtotalCompra = validarDinero(subtotalCompra, "El subtotal");
        compra.setSubtotal(subtotalCompra);
        compra.setTotal(calcularTotal(
            subtotalCompra,
            compra.getIgv(),
            compra.getGastosAdicionales()
        ));
    }

    private DetalleCompra crearDetalle(CompraDetalleRequest item) {
        Producto producto = productoRepository.findById(item.idProducto())
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe el producto solicitado: " + item.idProducto()
            ));
        if (producto.getEstado() != EstadoCatalogo.ACTIVO) {
            throw new OperacionNoPermitidaException(
                "No se puede comprar un producto inactivo: " + producto.getCodigoInterno()
            );
        }
        UnidadMedida unidad = unidadMedidaRepository.findById(item.idUnidadMedida())
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la unidad de medida solicitada: " + item.idUnidadMedida()
            ));
        if (unidad.getEstado() != EstadoCatalogo.ACTIVO) {
            throw new OperacionNoPermitidaException(
                "No se puede usar una unidad de medida inactiva"
            );
        }
        validarUnidadDelProducto(producto, unidad);

        BigDecimal cantidad = item.cantidad().setScale(ESCALA_CANTIDAD);
        if (!unidad.isPermiteDecimales() && cantidad.stripTrailingZeros().scale() > 0) {
            throw new SolicitudInvalidaException(
                "La unidad " + unidad.getCodigo() + " no admite cantidades decimales"
            );
        }
        BigDecimal precio = dinero(item.precioCompra());
        BigDecimal subtotal = validarDinero(
            cantidad.multiply(precio).setScale(ESCALA_DINERO, RoundingMode.HALF_UP),
            "El subtotal del detalle"
        );

        DetalleCompra detalle = new DetalleCompra();
        detalle.setProducto(producto);
        detalle.setUnidadMedida(unidad);
        detalle.setCantidad(cantidad);
        detalle.setPrecioCompra(precio);
        detalle.setSubtotal(subtotal);
        return detalle;
    }

    private void validarUnidadDelProducto(Producto producto, UnidadMedida unidad) {
        Long idBase = producto.getUnidadBase().getId();
        if (idBase.equals(unidad.getId())) {
            return;
        }
        boolean conversionActiva = conversionRepository
            .findByProductoIdAndUnidadOrigenIdAndUnidadDestinoIdAndEstado(
                producto.getId(),
                unidad.getId(),
                idBase,
                EstadoCatalogo.ACTIVO
            ).isPresent()
            || conversionRepository
                .findByProductoIdAndUnidadOrigenIdAndUnidadDestinoIdAndEstado(
                    producto.getId(),
                    idBase,
                    unidad.getId(),
                    EstadoCatalogo.ACTIVO
                ).isPresent();
        if (!conversionActiva) {
            throw new SolicitudInvalidaException(
                "La unidad " + unidad.getCodigo()
                    + " no es la unidad base ni tiene una conversión activa para el producto "
                    + producto.getCodigoInterno()
            );
        }
    }

    private Specification<Compra> crearFiltros(
        Long idProveedor,
        EstadoCompra estado,
        LocalDate desde,
        LocalDate hasta
    ) {
        return (root, query, builder) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (idProveedor != null) {
                condiciones.add(builder.equal(root.get("proveedor").get("id"), idProveedor));
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

    private void validarComprobanteDisponible(
        Long idProveedor,
        String tipoComprobante,
        String numeroComprobante,
        Long idActual
    ) {
        if (tipoComprobante == null) {
            return;
        }
        boolean existe = idActual == null
            ? compraRepository
                .existsByProveedorIdAndTipoComprobanteIgnoreCaseAndNumeroComprobanteIgnoreCase(
                    idProveedor,
                    tipoComprobante,
                    numeroComprobante
                )
            : compraRepository
                .existsByProveedorIdAndTipoComprobanteIgnoreCaseAndNumeroComprobanteIgnoreCaseAndIdNot(
                    idProveedor,
                    tipoComprobante,
                    numeroComprobante,
                    idActual
                );
        if (existe) {
            throw new ConflictoNegocioException(
                "Ya existe una compra de ese proveedor con el mismo comprobante"
            );
        }
    }

    private Proveedor buscarProveedorActivo(Long id) {
        Proveedor proveedor = proveedorRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe el proveedor solicitado"
            ));
        if (proveedor.getEstado() != EstadoCatalogo.ACTIVO) {
            throw new OperacionNoPermitidaException(
                "No se puede registrar una compra con un proveedor inactivo"
            );
        }
        return proveedor;
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

    private Compra buscarCompraDetallada(Long id) {
        return compraRepository.findDetalleById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la compra solicitada"
            ));
    }

    private void validarEditable(Compra compra) {
        if (compra.getEstado() != EstadoCompra.REGISTRADA) {
            throw new OperacionNoPermitidaException(
                "Solo se puede modificar o anular una compra REGISTRADA"
            );
        }
    }

    private BigDecimal calcularTotal(
        BigDecimal subtotal,
        BigDecimal igv,
        BigDecimal gastos
    ) {
        return validarDinero(subtotal.add(igv).add(gastos), "El total");
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

    private String normalizarMayusculas(String texto) {
        return texto == null || texto.isBlank()
            ? null
            : texto.strip().toUpperCase(Locale.ROOT);
    }

    private record DetalleClave(Long idProducto, Long idUnidadMedida) {
    }
}
