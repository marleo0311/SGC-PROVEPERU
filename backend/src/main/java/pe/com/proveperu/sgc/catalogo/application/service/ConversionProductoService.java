package pe.com.proveperu.sgc.catalogo.application.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.catalogo.api.dto.ConversionCrearRequest;
import pe.com.proveperu.sgc.catalogo.api.dto.ConversionResponse;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.catalogo.domain.model.Producto;
import pe.com.proveperu.sgc.catalogo.domain.model.ProductoUnidadConversion;
import pe.com.proveperu.sgc.catalogo.domain.model.UnidadMedida;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.ProductoRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.ProductoUnidadConversionRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.UnidadMedidaRepository;
import pe.com.proveperu.sgc.security.application.exception.ConflictoNegocioException;
import pe.com.proveperu.sgc.security.application.exception.OperacionNoPermitidaException;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;

@Service
@RequiredArgsConstructor
public class ConversionProductoService {

    private final ProductoUnidadConversionRepository conversionRepository;
    private final ProductoRepository productoRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;

    @Transactional(readOnly = true)
    public List<ConversionResponse> listar(Long idProducto) {
        validarProductoExiste(idProducto);
        return conversionRepository
            .findAllByProductoIdOrderByUnidadOrigenNombreAscUnidadDestinoNombreAsc(idProducto)
            .stream()
            .map(ConversionResponse::from)
            .toList();
    }

    @Transactional
    public ConversionResponse crear(Long idProducto, ConversionCrearRequest request) {
        Producto producto = buscarProductoActivo(idProducto);
        UnidadMedida origen = buscarUnidadActiva(request.idUnidadOrigen());
        UnidadMedida destino = buscarUnidadActiva(request.idUnidadDestino());
        validarConversionDisponible(idProducto, origen.getId(), destino.getId());

        ProductoUnidadConversion conversion = new ProductoUnidadConversion();
        conversion.setProducto(producto);
        conversion.setUnidadOrigen(origen);
        conversion.setUnidadDestino(destino);
        conversion.setFactorConversion(request.factorConversion());
        conversion.setEstado(EstadoCatalogo.ACTIVO);
        return ConversionResponse.from(conversionRepository.save(conversion));
    }

    private void validarProductoExiste(Long idProducto) {
        if (!productoRepository.existsById(idProducto)) {
            throw new RecursoNoEncontradoException("No existe el producto solicitado");
        }
    }

    private Producto buscarProductoActivo(Long idProducto) {
        Producto producto = productoRepository.findById(idProducto)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto solicitado"));
        if (producto.getEstado() != EstadoCatalogo.ACTIVO) {
            throw new OperacionNoPermitidaException("No se pueden registrar conversiones en un producto inactivo");
        }
        return producto;
    }

    private UnidadMedida buscarUnidadActiva(Long idUnidad) {
        UnidadMedida unidad = unidadMedidaRepository.findById(idUnidad)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe una de las unidades solicitadas"));
        if (unidad.getEstado() != EstadoCatalogo.ACTIVO) {
            throw new OperacionNoPermitidaException("No se puede utilizar una unidad de medida inactiva");
        }
        return unidad;
    }

    private void validarConversionDisponible(Long idProducto, Long idOrigen, Long idDestino) {
        boolean existe = conversionRepository.existsByProductoIdAndUnidadOrigenIdAndUnidadDestinoId(
            idProducto,
            idOrigen,
            idDestino
        ) || conversionRepository.existsByProductoIdAndUnidadOrigenIdAndUnidadDestinoId(
            idProducto,
            idDestino,
            idOrigen
        );
        if (existe) {
            throw new ConflictoNegocioException("Ya existe una conversión entre esas unidades para el producto");
        }
    }
}
