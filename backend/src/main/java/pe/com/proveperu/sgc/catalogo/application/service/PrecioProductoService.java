package pe.com.proveperu.sgc.catalogo.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.catalogo.api.dto.PrecioCrearRequest;
import pe.com.proveperu.sgc.catalogo.api.dto.PrecioResponse;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.catalogo.domain.model.PrecioProducto;
import pe.com.proveperu.sgc.catalogo.domain.model.Producto;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.PrecioProductoRepository;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.ProductoRepository;
import pe.com.proveperu.sgc.security.application.exception.ConflictoNegocioException;
import pe.com.proveperu.sgc.security.application.exception.OperacionNoPermitidaException;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;

@Service
@RequiredArgsConstructor
public class PrecioProductoService {

    private final PrecioProductoRepository precioRepository;
    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public List<PrecioResponse> listar(Long idProducto) {
        validarProductoExiste(idProducto);
        return precioRepository.findAllByProductoIdOrderByTipoPrecioAscVigenteDesdeDesc(idProducto)
            .stream()
            .map(PrecioResponse::from)
            .toList();
    }

    @Transactional
    public PrecioResponse crear(Long idProducto, PrecioCrearRequest request) {
        Producto producto = buscarProductoActivo(idProducto);
        String tipoPrecio = request.tipoPrecio().strip().toUpperCase(Locale.ROOT);
        cerrarPrecioAbiertoAnterior(
            idProducto,
            tipoPrecio,
            request.vigenteDesde(),
            request.vigenteHasta()
        );

        PrecioProducto precio = new PrecioProducto();
        precio.setProducto(producto);
        precio.setTipoPrecio(tipoPrecio);
        precio.setMonto(request.monto());
        precio.setVigenteDesde(request.vigenteDesde());
        precio.setVigenteHasta(request.vigenteHasta());
        precio.setEstado(EstadoCatalogo.ACTIVO);
        return PrecioResponse.from(precioRepository.save(precio));
    }

    @Transactional
    public void actualizarPrecioVigente(
        Producto producto,
        String tipoPrecio,
        BigDecimal nuevoMonto,
        LocalDate fecha
    ) {
        if (nuevoMonto == null) {
            return;
        }

        List<PrecioProducto> vigentes = precioRepository.buscarVigentes(
            producto.getId(),
            tipoPrecio,
            fecha,
            EstadoCatalogo.ACTIVO
        );
        PrecioProducto precioActual = vigentes.isEmpty() ? null : vigentes.getFirst();
        if (precioActual != null && precioActual.getMonto().compareTo(nuevoMonto) == 0) {
            return;
        }
        if (producto.getEstado() != EstadoCatalogo.ACTIVO) {
            throw new OperacionNoPermitidaException("No se pueden modificar precios de un producto inactivo");
        }

        if (precioActual != null && precioActual.getVigenteDesde().isEqual(fecha)) {
            precioActual.setMonto(nuevoMonto);
            return;
        }

        cerrarPrecioAbiertoAnterior(producto.getId(), tipoPrecio, fecha, null);
        PrecioProducto nuevoPrecio = new PrecioProducto();
        nuevoPrecio.setProducto(producto);
        nuevoPrecio.setTipoPrecio(tipoPrecio);
        nuevoPrecio.setMonto(nuevoMonto);
        nuevoPrecio.setVigenteDesde(fecha);
        nuevoPrecio.setEstado(EstadoCatalogo.ACTIVO);
        precioRepository.save(nuevoPrecio);
    }

    private void cerrarPrecioAbiertoAnterior(
        Long idProducto,
        String tipoPrecio,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta
    ) {
        List<PrecioProducto> solapados = precioRepository.buscarSolapados(
            idProducto,
            tipoPrecio,
            vigenteDesde,
            vigenteHasta,
            EstadoCatalogo.ACTIVO
        );
        for (PrecioProducto existente : solapados) {
            if (existente.getVigenteHasta() == null
                && existente.getVigenteDesde().isBefore(vigenteDesde)) {
                existente.setVigenteHasta(vigenteDesde.minusDays(1));
            } else {
                throw new ConflictoNegocioException(
                    "La vigencia del precio se superpone con otro precio activo del mismo tipo"
                );
            }
        }
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
            throw new OperacionNoPermitidaException("No se pueden registrar precios en un producto inactivo");
        }
        return producto;
    }
}
