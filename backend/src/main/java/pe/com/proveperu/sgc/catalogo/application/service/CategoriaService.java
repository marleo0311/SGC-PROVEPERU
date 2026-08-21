package pe.com.proveperu.sgc.catalogo.application.service;

import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.catalogo.api.dto.CategoriaActualizarRequest;
import pe.com.proveperu.sgc.catalogo.api.dto.CategoriaCrearRequest;
import pe.com.proveperu.sgc.catalogo.api.dto.CategoriaResponse;
import pe.com.proveperu.sgc.catalogo.domain.model.Categoria;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.catalogo.infrastructure.persistence.CategoriaRepository;
import pe.com.proveperu.sgc.security.application.exception.ConflictoNegocioException;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;

@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar(String buscar, EstadoCatalogo estado) {
        String criterio = normalizarBusqueda(buscar);
        return categoriaRepository.findAllByOrderByNombreAsc().stream()
            .filter(categoria -> estado == null || categoria.getEstado() == estado)
            .filter(categoria -> criterio.isEmpty()
                || categoria.getNombre().toLowerCase(Locale.ROOT).contains(criterio))
            .map(CategoriaResponse::from)
            .toList();
    }

    @Transactional
    public CategoriaResponse crear(CategoriaCrearRequest request) {
        String nombre = request.nombre().strip();
        validarNombreDisponible(nombre, null);

        Categoria categoria = new Categoria();
        categoria.setNombre(nombre);
        categoria.setDescripcion(normalizarDescripcion(request.descripcion()));
        categoria.setEstado(EstadoCatalogo.ACTIVO);
        return CategoriaResponse.from(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaActualizarRequest request) {
        Categoria categoria = buscar(id);
        String nombre = request.nombre().strip();
        validarNombreDisponible(nombre, id);
        categoria.setNombre(nombre);
        categoria.setDescripcion(normalizarDescripcion(request.descripcion()));
        return CategoriaResponse.from(categoria);
    }

    @Transactional
    public CategoriaResponse cambiarEstado(Long id, EstadoCatalogo estado) {
        Categoria categoria = buscar(id);
        categoria.setEstado(estado);
        return CategoriaResponse.from(categoria);
    }

    private Categoria buscar(Long id) {
        return categoriaRepository.findById(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe la categoría solicitada"));
    }

    private void validarNombreDisponible(String nombre, Long idActual) {
        boolean existe = idActual == null
            ? categoriaRepository.existsByNombreIgnoreCase(nombre)
            : categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, idActual);
        if (existe) {
            throw new ConflictoNegocioException("Ya existe una categoría con ese nombre");
        }
    }

    private String normalizarBusqueda(String buscar) {
        return buscar == null ? "" : buscar.strip().toLowerCase(Locale.ROOT);
    }

    private String normalizarDescripcion(String descripcion) {
        return descripcion == null || descripcion.isBlank() ? null : descripcion.strip();
    }
}
