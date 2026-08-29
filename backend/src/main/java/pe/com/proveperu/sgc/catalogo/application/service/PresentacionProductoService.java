package pe.com.proveperu.sgc.catalogo.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.catalogo.api.dto.PresentacionProductoGuardarRequest;
import pe.com.proveperu.sgc.catalogo.api.dto.PresentacionProductoResponse;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.catalogo.domain.model.PresentacionProducto;
import pe.com.proveperu.sgc.catalogo.domain.model.Producto;
import pe.com.proveperu.sgc.catalogo.domain.model.UnidadMedida;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.PresentacionProductoRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.ProductoRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.UnidadMedidaRepository;
import pe.com.proveperu.sgc.security.application.exception.ConflictoNegocioException;
import pe.com.proveperu.sgc.security.application.exception.OperacionNoPermitidaException;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;
import pe.com.proveperu.sgc.shared.application.exception.SolicitudInvalidaException;

@Service
@RequiredArgsConstructor
public class PresentacionProductoService {

    private final PresentacionProductoRepository presentacionRepository;
    private final ProductoRepository productoRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;

    @Transactional(readOnly = true)
    public List<PresentacionProductoResponse> listar(Long idProducto) {
        exigirProducto(idProducto);
        return presentacionRepository.findAllByProductoIdOrderByNombreAsc(idProducto)
            .stream()
            .map(PresentacionProductoResponse::from)
            .toList();
    }

    @Transactional
    public PresentacionProductoResponse crear(
        Long idProducto,
        PresentacionProductoGuardarRequest request
    ) {
        Producto producto = exigirProducto(idProducto);
        UnidadMedida unidad = exigirUnidadActiva(request.idUnidadMedida());
        validar(producto, unidad, request, null);
        PresentacionProducto presentacion = new PresentacionProducto();
        presentacion.setProducto(producto);
        presentacion.setUnidadMedida(unidad);
        aplicar(presentacion, request);
        return PresentacionProductoResponse.from(presentacionRepository.save(presentacion));
    }

    @Transactional
    public PresentacionProductoResponse actualizar(
        Long idProducto,
        Long id,
        PresentacionProductoGuardarRequest request
    ) {
        PresentacionProducto presentacion = buscar(idProducto, id);
        UnidadMedida unidad = exigirUnidadActiva(request.idUnidadMedida());
        validar(presentacion.getProducto(), unidad, request, id);
        presentacion.setUnidadMedida(unidad);
        aplicar(presentacion, request);
        return PresentacionProductoResponse.from(presentacion);
    }

    @Transactional
    public PresentacionProductoResponse cambiarEstado(
        Long idProducto,
        Long id,
        EstadoCatalogo estado
    ) {
        PresentacionProducto presentacion = buscar(idProducto, id);
        presentacion.setEstado(estado);
        return PresentacionProductoResponse.from(presentacion);
    }

    private void aplicar(
        PresentacionProducto presentacion,
        PresentacionProductoGuardarRequest request
    ) {
        presentacion.setNombre(request.nombre().strip());
        presentacion.setContenidoVariable(request.contenidoVariable());
        presentacion.setContenidoBasePredeterminado(
            request.contenidoBasePredeterminado() == null
                ? null
                : request.contenidoBasePredeterminado().setScale(
                    3,
                    RoundingMode.UNNECESSARY
                )
        );
        if (presentacion.getEstado() == null) {
            presentacion.setEstado(EstadoCatalogo.ACTIVO);
        }
    }

    private void validar(
        Producto producto,
        UnidadMedida unidad,
        PresentacionProductoGuardarRequest request,
        Long idActual
    ) {
        if (producto.getUnidadBase().getId().equals(unidad.getId())) {
            throw new SolicitudInvalidaException(
                "La presentación debe usar una unidad distinta de la unidad base"
            );
        }
        boolean repetida = idActual == null
            ? presentacionRepository.existsByProductoIdAndUnidadMedidaId(
                producto.getId(), unidad.getId())
            : presentacionRepository.existsByProductoIdAndUnidadMedidaIdAndIdNot(
                producto.getId(), unidad.getId(), idActual);
        if (repetida) {
            throw new ConflictoNegocioException(
                "El producto ya tiene una presentación con la unidad " + unidad.getCodigo()
            );
        }
        BigDecimal contenido = request.contenidoBasePredeterminado();
        if (contenido != null
            && !producto.getUnidadBase().isPermiteDecimales()
            && contenido.stripTrailingZeros().scale() > 0) {
            throw new SolicitudInvalidaException(
                "La unidad base " + producto.getUnidadBase().getCodigo()
                    + " no admite contenidos decimales"
            );
        }
    }

    private PresentacionProducto buscar(Long idProducto, Long id) {
        return presentacionRepository.findByIdAndProductoId(id, idProducto)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la presentación solicitada para el producto"
            ));
    }

    private Producto exigirProducto(Long id) {
        Producto producto = productoRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe el producto solicitado"
            ));
        if (producto.getEstado() != EstadoCatalogo.ACTIVO) {
            throw new OperacionNoPermitidaException(
                "No se pueden administrar presentaciones de un producto inactivo"
            );
        }
        return producto;
    }

    private UnidadMedida exigirUnidadActiva(Long id) {
        UnidadMedida unidad = unidadMedidaRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la unidad de presentación solicitada"
            ));
        if (unidad.getEstado() != EstadoCatalogo.ACTIVO) {
            throw new OperacionNoPermitidaException(
                "No se puede usar una unidad de medida inactiva"
            );
        }
        return unidad;
    }
}
