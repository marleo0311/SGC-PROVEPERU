package pe.com.proveperu.sgc.transporte.application.service;

import jakarta.persistence.criteria.Predicate;
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
import pe.com.proveperu.sgc.security.application.exception.ConflictoNegocioException;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;
import pe.com.proveperu.sgc.shared.api.dto.PaginaResponse;
import pe.com.proveperu.sgc.transporte.api.dto.GastoResponse;
import pe.com.proveperu.sgc.transporte.api.dto.TransportistaGuardarRequest;
import pe.com.proveperu.sgc.transporte.api.dto.TransportistaResponse;
import pe.com.proveperu.sgc.transporte.domain.model.TipoDocumentoTransportista;
import pe.com.proveperu.sgc.transporte.domain.model.Transportista;
import pe.com.proveperu.sgc.transporte.infrastructure.persistence.GastoRepository;
import pe.com.proveperu.sgc.transporte.infrastructure.persistence.TransportistaRepository;

@Service
@RequiredArgsConstructor
public class TransportistaService {

    private final TransportistaRepository transportistaRepository;
    private final GastoRepository gastoRepository;

    @Transactional(readOnly = true)
    public PaginaResponse<TransportistaResponse> listar(
        String buscar,
        EstadoCatalogo estado,
        Pageable pageable
    ) {
        Specification<Transportista> filtros = crearFiltros(buscar, estado);
        Page<TransportistaResponse> pagina = transportistaRepository.findAll(filtros, pageable)
            .map(TransportistaResponse::from);
        return PaginaResponse.from(pagina);
    }

    @Transactional(readOnly = true)
    public TransportistaResponse obtener(Long id) {
        return TransportistaResponse.from(buscarTransportista(id));
    }

    @Transactional
    public TransportistaResponse crear(TransportistaGuardarRequest request) {
        String documento = normalizarDocumento(request.numeroDocumento());
        validarDocumentoDisponible(request.tipoDocumento(), documento, null);

        Transportista transportista = new Transportista();
        aplicarDatos(transportista, request, documento);
        transportista.setEstado(EstadoCatalogo.ACTIVO);
        return TransportistaResponse.from(transportistaRepository.save(transportista));
    }

    @Transactional
    public TransportistaResponse actualizar(
        Long id,
        TransportistaGuardarRequest request
    ) {
        Transportista transportista = buscarTransportista(id);
        String documento = normalizarDocumento(request.numeroDocumento());
        validarDocumentoDisponible(request.tipoDocumento(), documento, id);
        aplicarDatos(transportista, request, documento);
        return TransportistaResponse.from(transportistaRepository.saveAndFlush(transportista));
    }

    @Transactional
    public TransportistaResponse cambiarEstado(Long id, EstadoCatalogo estado) {
        Transportista transportista = buscarTransportista(id);
        transportista.setEstado(estado);
        return TransportistaResponse.from(transportistaRepository.saveAndFlush(transportista));
    }

    @Transactional(readOnly = true)
    public List<GastoResponse> listarGastos(Long id) {
        buscarTransportista(id);
        return gastoRepository.findAllByTransportistaIdOrderByFechaDescIdDesc(id)
            .stream()
            .map(GastoResponse::from)
            .toList();
    }

    private Specification<Transportista> crearFiltros(
        String buscar,
        EstadoCatalogo estado
    ) {
        return (root, query, builder) -> {
            List<Predicate> condiciones = new ArrayList<>();
            String criterio = buscar == null ? "" : buscar.strip().toLowerCase(Locale.ROOT);
            if (!criterio.isEmpty()) {
                String patron = "%" + criterio + "%";
                condiciones.add(builder.or(
                    builder.like(builder.lower(root.get("numeroDocumento")), patron),
                    builder.like(builder.lower(root.get("nombreRazonSocial")), patron),
                    builder.like(builder.lower(root.get("empresaTransporte")), patron)
                ));
            }
            if (estado != null) {
                condiciones.add(builder.equal(root.get("estado"), estado));
            }
            return builder.and(condiciones.toArray(Predicate[]::new));
        };
    }

    private void aplicarDatos(
        Transportista transportista,
        TransportistaGuardarRequest request,
        String documento
    ) {
        transportista.setTipoDocumento(request.tipoDocumento());
        transportista.setNumeroDocumento(documento);
        transportista.setNombreRazonSocial(request.nombreRazonSocial().strip());
        transportista.setEmpresaTransporte(
            normalizarTextoOpcional(request.empresaTransporte())
        );
        transportista.setTelefono(normalizarTextoOpcional(request.telefono()));
        transportista.setDireccion(normalizarTextoOpcional(request.direccion()));
    }

    private void validarDocumentoDisponible(
        TipoDocumentoTransportista tipoDocumento,
        String numeroDocumento,
        Long idActual
    ) {
        if (tipoDocumento == null || numeroDocumento == null) {
            return;
        }
        boolean existe = idActual == null
            ? transportistaRepository.existsByTipoDocumentoAndNumeroDocumento(
                tipoDocumento,
                numeroDocumento
            )
            : transportistaRepository.existsByTipoDocumentoAndNumeroDocumentoAndIdNot(
                tipoDocumento,
                numeroDocumento,
                idActual
            );
        if (existe) {
            throw new ConflictoNegocioException(
                "Ya existe un transportista con ese documento"
            );
        }
    }

    private Transportista buscarTransportista(Long id) {
        return transportistaRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe el transportista solicitado"
            ));
    }

    private String normalizarDocumento(String documento) {
        return documento == null || documento.isBlank() ? null : documento.strip();
    }

    private String normalizarTextoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }
}
