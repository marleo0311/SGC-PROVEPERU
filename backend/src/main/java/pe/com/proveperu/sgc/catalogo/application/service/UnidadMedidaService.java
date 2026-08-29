package pe.com.proveperu.sgc.catalogo.application.service;

import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.catalogo.api.dto.UnidadMedidaActualizarRequest;
import pe.com.proveperu.sgc.catalogo.api.dto.UnidadMedidaCrearRequest;
import pe.com.proveperu.sgc.catalogo.api.dto.UnidadMedidaResponse;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.catalogo.domain.model.UnidadMedida;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.UnidadMedidaRepository;
import pe.com.proveperu.sgc.security.application.exception.ConflictoNegocioException;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;

@Service
@RequiredArgsConstructor
public class UnidadMedidaService {

    private final UnidadMedidaRepository unidadMedidaRepository;

    @Transactional(readOnly = true)
    public List<UnidadMedidaResponse> listar(String buscar, EstadoCatalogo estado) {
        String criterio = normalizarBusqueda(buscar);
        return unidadMedidaRepository.findAllByOrderByNombreAsc().stream()
            .filter(unidad -> estado == null || unidad.getEstado() == estado)
            .filter(unidad -> criterio.isEmpty()
                || unidad.getNombre().toLowerCase(Locale.ROOT).contains(criterio)
                || unidad.getCodigo().toLowerCase(Locale.ROOT).contains(criterio))
            .map(UnidadMedidaResponse::from)
            .toList();
    }

    @Transactional
    public UnidadMedidaResponse crear(UnidadMedidaCrearRequest request) {
        String codigo = normalizarCodigo(request.codigo());
        validarCodigoDisponible(codigo, null);

        UnidadMedida unidad = new UnidadMedida();
        unidad.setCodigo(codigo);
        unidad.setNombre(request.nombre().strip());
        unidad.setCodigoSunat(normalizarCodigoSunat(request.codigoSunat()));
        unidad.setPermiteDecimales(request.permiteDecimales());
        unidad.setEstado(EstadoCatalogo.ACTIVO);
        return UnidadMedidaResponse.from(unidadMedidaRepository.save(unidad));
    }

    @Transactional
    public UnidadMedidaResponse actualizar(Long id, UnidadMedidaActualizarRequest request) {
        UnidadMedida unidad = buscar(id);
        String codigo = normalizarCodigo(request.codigo());
        validarCodigoDisponible(codigo, id);
        unidad.setCodigo(codigo);
        unidad.setNombre(request.nombre().strip());
        unidad.setCodigoSunat(normalizarCodigoSunat(request.codigoSunat()));
        unidad.setPermiteDecimales(request.permiteDecimales());
        unidad.setEstado(request.estado());
        return UnidadMedidaResponse.from(unidad);
    }

    private UnidadMedida buscar(Long id) {
        return unidadMedidaRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la unidad de medida solicitada"));
    }

    private void validarCodigoDisponible(String codigo, Long idActual) {
        boolean existe = idActual == null
            ? unidadMedidaRepository.existsByCodigoIgnoreCase(codigo)
            : unidadMedidaRepository.existsByCodigoIgnoreCaseAndIdNot(codigo, idActual);
        if (existe) {
            throw new ConflictoNegocioException("Ya existe una unidad de medida con ese código");
        }
    }

    private String normalizarCodigo(String codigo) {
        return codigo.strip().toUpperCase(Locale.ROOT);
    }

    private String normalizarCodigoSunat(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            return "NIU";
        }
        String normalizado = codigo.strip().toUpperCase(Locale.ROOT);
        if (!normalizado.matches("[A-Z0-9]{2,3}")) {
            throw new pe.com.proveperu.sgc.shared.application.exception.SolicitudInvalidaException(
                "El código SUNAT debe contener 2 o 3 letras o números"
            );
        }
        return normalizado;
    }

    private String normalizarBusqueda(String buscar) {
        return buscar == null ? "" : buscar.strip().toLowerCase(Locale.ROOT);
    }
}
