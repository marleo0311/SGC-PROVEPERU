package pe.com.proveperu.sgc.catalogo.application.service;

import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.catalogo.api.dto.MarcaActualizarRequest;
import pe.com.proveperu.sgc.catalogo.api.dto.MarcaCrearRequest;
import pe.com.proveperu.sgc.catalogo.api.dto.MarcaResponse;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.catalogo.domain.model.Marca;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.MarcaRepository;
import pe.com.proveperu.sgc.security.application.exception.ConflictoNegocioException;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;

@Service
@RequiredArgsConstructor
public class MarcaService {

    private final MarcaRepository marcaRepository;

    @Transactional(readOnly = true)
    public List<MarcaResponse> listar(String buscar, EstadoCatalogo estado) {
        String criterio = normalizarBusqueda(buscar);
        return marcaRepository.findAllByOrderByNombreAsc().stream()
            .filter(marca -> estado == null || marca.getEstado() == estado)
            .filter(marca -> criterio.isEmpty()
                || marca.getNombre().toLowerCase(Locale.ROOT).contains(criterio))
            .map(MarcaResponse::from)
            .toList();
    }

    @Transactional
    public MarcaResponse crear(MarcaCrearRequest request) {
        String nombre = request.nombre().strip();
        validarNombreDisponible(nombre, null);

        Marca marca = new Marca();
        marca.setNombre(nombre);
        marca.setEstado(EstadoCatalogo.ACTIVO);
        return MarcaResponse.from(marcaRepository.save(marca));
    }

    @Transactional
    public MarcaResponse actualizar(Long id, MarcaActualizarRequest request) {
        Marca marca = buscar(id);
        String nombre = request.nombre().strip();
        validarNombreDisponible(nombre, id);
        marca.setNombre(nombre);
        marca.setEstado(request.estado());
        return MarcaResponse.from(marca);
    }

    private Marca buscar(Long id) {
        return marcaRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la marca solicitada"));
    }

    private void validarNombreDisponible(String nombre, Long idActual) {
        boolean existe = idActual == null
            ? marcaRepository.existsByNombreIgnoreCase(nombre)
            : marcaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, idActual);
        if (existe) {
            throw new ConflictoNegocioException("Ya existe una marca con ese nombre");
        }
    }

    private String normalizarBusqueda(String buscar) {
        return buscar == null ? "" : buscar.strip().toLowerCase(Locale.ROOT);
    }
}
