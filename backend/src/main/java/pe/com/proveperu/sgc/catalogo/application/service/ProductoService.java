package pe.com.proveperu.sgc.catalogo.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.catalogo.api.dto.ProductoActualizarRequest;
import pe.com.proveperu.sgc.catalogo.api.dto.ProductoCrearRequest;
import pe.com.proveperu.sgc.catalogo.api.dto.ProductoResponse;
import pe.com.proveperu.sgc.catalogo.domain.model.Categoria;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.catalogo.domain.model.Marca;
import pe.com.proveperu.sgc.catalogo.domain.model.PrecioProducto;
import pe.com.proveperu.sgc.catalogo.domain.model.Producto;
import pe.com.proveperu.sgc.catalogo.domain.model.UnidadMedida;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.CategoriaRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.MarcaRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.PrecioProductoRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.ProductoRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.UnidadMedidaRepository;
import pe.com.proveperu.sgc.security.application.exception.ConflictoNegocioException;
import pe.com.proveperu.sgc.security.application.exception.OperacionNoPermitidaException;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;
import pe.com.proveperu.sgc.shared.api.dto.PaginaResponse;

@Service
@RequiredArgsConstructor
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final MarcaRepository marcaRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;
    private final PrecioProductoRepository precioProductoRepository;

    @Transactional(readOnly = true)
    public PaginaResponse<ProductoResponse> listar(
        String buscar,
        EstadoCatalogo estado,
        Long idCategoria,
        Pageable pageable
    ) {
        String criterio = buscar == null ? "" : buscar.strip();
        Page<ProductoResponse> pagina = productoRepository.buscar(criterio, estado, idCategoria, pageable)
            .map(ProductoResponse::from);
        return PaginaResponse.from(pagina);
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return ProductoResponse.from(buscarProducto(id));
    }

    @Transactional
    public ProductoResponse crear(ProductoCrearRequest request) {
        String codigoInterno = normalizarCodigo(request.codigoInterno());
        String codigoBarras = normalizarCodigoOpcional(request.codigoBarras());
        validarCodigosDisponibles(codigoInterno, codigoBarras, null);

        Producto producto = new Producto();
        producto.setCodigoInterno(codigoInterno);
        producto.setCodigoBarras(codigoBarras);
        producto.setNombre(request.nombre().strip());
        producto.setDescripcion(normalizarTextoOpcional(request.descripcion()));
        producto.setCategoria(buscarCategoriaActiva(request.idCategoria()));
        producto.setMarca(buscarMarcaActiva(request.idMarca()));
        producto.setUnidadBase(buscarUnidadActiva(request.idUnidadBase()));
        producto.setStockMinimo(request.stockMinimo());
        producto.setEstado(EstadoCatalogo.ACTIVO);
        producto = productoRepository.save(producto);

        LocalDate hoy = LocalDate.now();
        registrarPrecioInicial(producto, "MINORISTA", request.precioMinorista(), hoy);
        registrarPrecioInicial(producto, "MAYORISTA", request.precioMayorista(), hoy);
        return ProductoResponse.from(producto);
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoActualizarRequest request) {
        Producto producto = buscarProducto(id);
        String codigoInterno = normalizarCodigo(request.codigoInterno());
        String codigoBarras = normalizarCodigoOpcional(request.codigoBarras());
        validarCodigosDisponibles(codigoInterno, codigoBarras, id);

        producto.setCodigoInterno(codigoInterno);
        producto.setCodigoBarras(codigoBarras);
        producto.setNombre(request.nombre().strip());
        producto.setDescripcion(normalizarTextoOpcional(request.descripcion()));
        producto.setCategoria(buscarCategoriaActiva(request.idCategoria()));
        producto.setMarca(buscarMarcaActiva(request.idMarca()));
        producto.setUnidadBase(buscarUnidadActiva(request.idUnidadBase()));
        producto.setStockMinimo(request.stockMinimo());
        return ProductoResponse.from(producto);
    }

    @Transactional
    public ProductoResponse cambiarEstado(Long id, EstadoCatalogo estado) {
        Producto producto = buscarProducto(id);
        producto.setEstado(estado);
        return ProductoResponse.from(producto);
    }

    private Producto buscarProducto(Long id) {
        return productoRepository.findByIdWithReferencias(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto solicitado"));
    }

    private Categoria buscarCategoriaActiva(Long id) {
        Categoria categoria = categoriaRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la categoría solicitada"));
        validarActivo(categoria.getEstado(), "categoría");
        return categoria;
    }

    private Marca buscarMarcaActiva(Long id) {
        if (id == null) {
            return null;
        }
        Marca marca = marcaRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la marca solicitada"));
        validarActivo(marca.getEstado(), "marca");
        return marca;
    }

    private UnidadMedida buscarUnidadActiva(Long id) {
        UnidadMedida unidad = unidadMedidaRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la unidad de medida solicitada"));
        validarActivo(unidad.getEstado(), "unidad de medida");
        return unidad;
    }

    private void validarActivo(EstadoCatalogo estado, String recurso) {
        if (estado != EstadoCatalogo.ACTIVO) {
            throw new OperacionNoPermitidaException("No se puede utilizar una " + recurso + " inactiva");
        }
    }

    private void validarCodigosDisponibles(String codigoInterno, String codigoBarras, Long idActual) {
        boolean codigoInternoExiste = idActual == null
            ? productoRepository.existsByCodigoInternoIgnoreCase(codigoInterno)
            : productoRepository.existsByCodigoInternoIgnoreCaseAndIdNot(codigoInterno, idActual);
        if (codigoInternoExiste) {
            throw new ConflictoNegocioException("Ya existe un producto con ese código interno");
        }

        if (codigoBarras == null) {
            return;
        }
        boolean codigoBarrasExiste = idActual == null
            ? productoRepository.existsByCodigoBarrasIgnoreCase(codigoBarras)
            : productoRepository.existsByCodigoBarrasIgnoreCaseAndIdNot(codigoBarras, idActual);
        if (codigoBarrasExiste) {
            throw new ConflictoNegocioException("Ya existe un producto con ese código de barras");
        }
    }

    private void registrarPrecioInicial(
        Producto producto,
        String tipoPrecio,
        BigDecimal monto,
        LocalDate vigenteDesde
    ) {
        if (monto == null) {
            return;
        }
        PrecioProducto precio = new PrecioProducto();
        precio.setProducto(producto);
        precio.setTipoPrecio(tipoPrecio);
        precio.setMonto(monto);
        precio.setVigenteDesde(vigenteDesde);
        precio.setEstado(EstadoCatalogo.ACTIVO);
        precioProductoRepository.save(precio);
    }

    private String normalizarCodigo(String codigo) {
        return codigo.strip().toUpperCase(Locale.ROOT);
    }

    private String normalizarCodigoOpcional(String codigo) {
        return codigo == null || codigo.isBlank() ? null : normalizarCodigo(codigo);
    }

    private String normalizarTextoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }
}
