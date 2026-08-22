package pe.com.proveperu.sgc.proveedor.application.service;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.compra.domain.model.Compra;
import pe.com.proveperu.sgc.compra.domain.model.CondicionPagoCompra;
import pe.com.proveperu.sgc.compra.domain.model.EstadoCompra;
import pe.com.proveperu.sgc.compra.infrastructure.persistence.CompraRepository;
import pe.com.proveperu.sgc.proveedor.api.dto.ProveedorCompraResponse;
import pe.com.proveperu.sgc.proveedor.api.dto.ProveedorGuardarRequest;
import pe.com.proveperu.sgc.proveedor.api.dto.ProveedorHistorialResponse;
import pe.com.proveperu.sgc.proveedor.api.dto.ProveedorResponse;
import pe.com.proveperu.sgc.proveedor.domain.model.Proveedor;
import pe.com.proveperu.sgc.proveedor.infrastructure.persistence.ProveedorRepository;
import pe.com.proveperu.sgc.security.application.exception.ConflictoNegocioException;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;
import pe.com.proveperu.sgc.shared.api.dto.PaginaResponse;

@Service
@RequiredArgsConstructor
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;
    private final CompraRepository compraRepository;

    @Transactional(readOnly = true)
    public PaginaResponse<ProveedorResponse> listar(
        String buscar,
        EstadoCatalogo estado,
        Pageable pageable
    ) {
        Specification<Proveedor> filtros = crearFiltros(buscar, estado);
        Page<ProveedorResponse> pagina = proveedorRepository.findAll(filtros, pageable)
            .map(ProveedorResponse::from);
        return PaginaResponse.from(pagina);
    }

    @Transactional(readOnly = true)
    public ProveedorResponse obtener(Long id) {
        return ProveedorResponse.from(buscarProveedor(id));
    }

    @Transactional
    public ProveedorResponse crear(ProveedorGuardarRequest request) {
        String ruc = request.ruc().strip();
        validarRucDisponible(ruc, null);

        Proveedor proveedor = new Proveedor();
        aplicarDatos(proveedor, request, ruc);
        proveedor.setEstado(EstadoCatalogo.ACTIVO);
        return ProveedorResponse.from(proveedorRepository.save(proveedor));
    }

    @Transactional
    public ProveedorResponse actualizar(Long id, ProveedorGuardarRequest request) {
        Proveedor proveedor = buscarProveedor(id);
        String ruc = request.ruc().strip();
        validarRucDisponible(ruc, id);
        aplicarDatos(proveedor, request, ruc);
        return ProveedorResponse.from(proveedorRepository.saveAndFlush(proveedor));
    }

    @Transactional
    public ProveedorResponse cambiarEstado(Long id, EstadoCatalogo estado) {
        Proveedor proveedor = buscarProveedor(id);
        proveedor.setEstado(estado);
        return ProveedorResponse.from(proveedorRepository.saveAndFlush(proveedor));
    }

    @Transactional(readOnly = true)
    public ProveedorHistorialResponse obtenerHistorialCompras(Long id) {
        Proveedor proveedor = buscarProveedor(id);
        List<Compra> compras = compraRepository
            .findAllByProveedorIdOrderByFechaDescIdDesc(id);
        List<ProveedorCompraResponse> historial = compras.stream()
            .map(this::mapearCompra)
            .toList();
        List<Compra> comprasVigentes = compras.stream()
            .filter(compra -> compra.getEstado() != EstadoCompra.ANULADA)
            .toList();
        ProveedorHistorialResponse.Resumen resumen = new ProveedorHistorialResponse.Resumen(
            comprasVigentes.size(),
            comprasVigentes.stream()
                .map(Compra::getTotal)
                .reduce(new BigDecimal("0.00"), BigDecimal::add),
            historial.stream()
                .map(ProveedorCompraResponse::saldoPendiente)
                .reduce(new BigDecimal("0.00"), BigDecimal::add),
            comprasVigentes.stream()
                .map(Compra::getFecha)
                .max(java.time.LocalDate::compareTo)
                .orElse(null)
        );
        return new ProveedorHistorialResponse(
            ProveedorResponse.from(proveedor),
            resumen,
            historial
        );
    }

    private ProveedorCompraResponse mapearCompra(Compra compra) {
        BigDecimal saldoPendiente = compra.getEstado() == EstadoCompra.ANULADA
            || compra.getCondicionPago() == CondicionPagoCompra.CONTADO
            ? new BigDecimal("0.00")
            : compra.getTotal();
        return new ProveedorCompraResponse(
            compra.getId(),
            compra.getTipoComprobante(),
            compra.getNumeroComprobante(),
            compra.getFecha(),
            compra.getEstado().name(),
            compra.getTotal(),
            saldoPendiente
        );
    }

    private Specification<Proveedor> crearFiltros(String buscar, EstadoCatalogo estado) {
        return (root, query, builder) -> {
            List<Predicate> condiciones = new ArrayList<>();
            String criterio = buscar == null ? "" : buscar.strip().toLowerCase(Locale.ROOT);
            if (!criterio.isEmpty()) {
                String patron = "%" + criterio + "%";
                condiciones.add(builder.or(
                    builder.like(builder.lower(root.get("ruc")), patron),
                    builder.like(builder.lower(root.get("razonSocial")), patron),
                    builder.like(builder.lower(root.get("nombreComercial")), patron),
                    builder.like(builder.lower(root.get("personaContacto")), patron)
                ));
            }
            if (estado != null) {
                condiciones.add(builder.equal(root.get("estado"), estado));
            }
            return builder.and(condiciones.toArray(Predicate[]::new));
        };
    }

    private void aplicarDatos(
        Proveedor proveedor,
        ProveedorGuardarRequest request,
        String ruc
    ) {
        proveedor.setRuc(ruc);
        proveedor.setRazonSocial(request.razonSocial().strip());
        proveedor.setNombreComercial(normalizarTextoOpcional(request.nombreComercial()));
        proveedor.setDireccion(normalizarTextoOpcional(request.direccion()));
        proveedor.setTelefono(normalizarTextoOpcional(request.telefono()));
        proveedor.setCorreo(normalizarCorreo(request.correo()));
        proveedor.setPersonaContacto(normalizarTextoOpcional(request.personaContacto()));
    }

    private void validarRucDisponible(String ruc, Long idActual) {
        boolean existe = idActual == null
            ? proveedorRepository.existsByRuc(ruc)
            : proveedorRepository.existsByRucAndIdNot(ruc, idActual);
        if (existe) {
            throw new ConflictoNegocioException("Ya existe un proveedor con ese RUC");
        }
    }

    private Proveedor buscarProveedor(Long id) {
        return proveedorRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe el proveedor solicitado"
            ));
    }

    private String normalizarTextoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }

    private String normalizarCorreo(String correo) {
        String normalizado = normalizarTextoOpcional(correo);
        return normalizado == null ? null : normalizado.toLowerCase(Locale.ROOT);
    }
}
