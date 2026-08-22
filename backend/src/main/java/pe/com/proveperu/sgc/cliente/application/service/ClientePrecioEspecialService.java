package pe.com.proveperu.sgc.cliente.application.service;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.catalogo.domain.model.Producto;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.ProductoRepository;
import pe.com.proveperu.sgc.cliente.api.dto.ClientePrecioEspecialRequest;
import pe.com.proveperu.sgc.cliente.api.dto.ClientePrecioEspecialResponse;
import pe.com.proveperu.sgc.cliente.domain.model.Cliente;
import pe.com.proveperu.sgc.cliente.domain.model.ClientePrecioEspecial;
import pe.com.proveperu.sgc.cliente.infrastructure.persistence.ClientePrecioEspecialRepository;
import pe.com.proveperu.sgc.cliente.infrastructure.persistence.ClienteRepository;
import pe.com.proveperu.sgc.security.application.exception.ConflictoNegocioException;
import pe.com.proveperu.sgc.security.application.exception.OperacionNoPermitidaException;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;

@Service
@RequiredArgsConstructor
public class ClientePrecioEspecialService {

    private static final LocalDate FECHA_MAXIMA = LocalDate.of(9999, 12, 31);

    private final ClientePrecioEspecialRepository precioRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;

    @Transactional(readOnly = true)
    public List<ClientePrecioEspecialResponse> listar(Long idCliente) {
        validarClienteExiste(idCliente);
        return precioRepository.findAllByClienteIdOrderByProductoNombreAscVigenteDesdeDesc(idCliente)
            .stream()
            .map(ClientePrecioEspecialResponse::from)
            .toList();
    }

    @Transactional
    public ClientePrecioEspecialResponse crear(
        Long idCliente,
        ClientePrecioEspecialRequest request
    ) {
        Cliente cliente = buscarClienteActivo(idCliente);
        Producto producto = buscarProductoActivo(request.idProducto());
        cerrarVigenciaAnterior(
            idCliente,
            request.idProducto(),
            request.vigenteDesde(),
            request.vigenteHasta()
        );

        ClientePrecioEspecial precio = new ClientePrecioEspecial();
        precio.setCliente(cliente);
        precio.setProducto(producto);
        precio.setPrecio(request.precio());
        precio.setVigenteDesde(request.vigenteDesde());
        precio.setVigenteHasta(request.vigenteHasta());
        precio.setEstado(EstadoCatalogo.ACTIVO);
        return ClientePrecioEspecialResponse.from(precioRepository.save(precio));
    }

    private void cerrarVigenciaAnterior(
        Long idCliente,
        Long idProducto,
        LocalDate nuevaFechaInicio,
        LocalDate nuevaFechaFin
    ) {
        List<ClientePrecioEspecial> preciosActivos = precioRepository
            .findAllByClienteIdAndProductoIdAndEstado(
                idCliente,
                idProducto,
                EstadoCatalogo.ACTIVO
            );
        for (ClientePrecioEspecial existente : preciosActivos) {
            if (!seSuperponen(
                existente.getVigenteDesde(),
                existente.getVigenteHasta(),
                nuevaFechaInicio,
                nuevaFechaFin
            )) {
                continue;
            }
            if (existente.getVigenteHasta() == null
                && existente.getVigenteDesde().isBefore(nuevaFechaInicio)) {
                existente.setVigenteHasta(nuevaFechaInicio.minusDays(1));
                continue;
            }
            throw new ConflictoNegocioException(
                "La vigencia del precio especial se superpone con otro precio activo"
            );
        }
    }

    private boolean seSuperponen(
        LocalDate inicioA,
        LocalDate finA,
        LocalDate inicioB,
        LocalDate finB
    ) {
        LocalDate finNormalizadoA = finA == null ? FECHA_MAXIMA : finA;
        LocalDate finNormalizadoB = finB == null ? FECHA_MAXIMA : finB;
        return !inicioA.isAfter(finNormalizadoB) && !inicioB.isAfter(finNormalizadoA);
    }

    private void validarClienteExiste(Long idCliente) {
        if (!clienteRepository.existsById(idCliente)) {
            throw new RecursoNoEncontradoException("No existe el cliente solicitado");
        }
    }

    private Cliente buscarClienteActivo(Long idCliente) {
        Cliente cliente = clienteRepository.findById(idCliente)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el cliente solicitado"));
        if (cliente.getEstado() != EstadoCatalogo.ACTIVO) {
            throw new OperacionNoPermitidaException(
                "No se pueden registrar precios especiales en un cliente inactivo"
            );
        }
        return cliente;
    }

    private Producto buscarProductoActivo(Long idProducto) {
        Producto producto = productoRepository.findById(idProducto)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el producto solicitado"));
        if (producto.getEstado() != EstadoCatalogo.ACTIVO) {
            throw new OperacionNoPermitidaException(
                "No se pueden registrar precios especiales en un producto inactivo"
            );
        }
        return producto;
    }
}
