package pe.com.proveperu.sgc.security.application.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.security.api.dto.RolCrearRequest;
import pe.com.proveperu.sgc.security.api.dto.RolDetalleResponse;
import pe.com.proveperu.sgc.security.api.dto.RolResumenResponse;
import pe.com.proveperu.sgc.security.application.exception.ConflictoNegocioException;
import pe.com.proveperu.sgc.security.application.exception.OperacionNoPermitidaException;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;
import pe.com.proveperu.sgc.security.domain.model.EstadoRegistro;
import pe.com.proveperu.sgc.security.domain.model.Permiso;
import pe.com.proveperu.sgc.security.domain.model.Rol;
import pe.com.proveperu.sgc.security.infrastructure.persistence.PermisoRepository;
import pe.com.proveperu.sgc.security.infrastructure.persistence.RolRepository;

@Service
@RequiredArgsConstructor
public class RolAdminService {

    private static final String ROL_ADMINISTRADOR = "Administrador";

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;

    @Transactional(readOnly = true)
    public List<RolResumenResponse> listar() {
        return rolRepository.findAllByOrderByNombreAsc().stream()
            .map(RolResumenResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public RolDetalleResponse obtener(Long id) {
        return RolDetalleResponse.from(buscarRol(id));
    }

    @Transactional
    public RolDetalleResponse crear(RolCrearRequest request) {
        String nombre = request.nombre().strip();
        if (rolRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictoNegocioException("Ya existe un rol con ese nombre");
        }

        Rol rol = new Rol();
        rol.setNombre(nombre);
        rol.setDescripcion(normalizarDescripcion(request.descripcion()));
        rol.setEstado(EstadoRegistro.ACTIVO);
        rol.setPermisos(cargarPermisos(request.idsPermisos()));
        return RolDetalleResponse.from(rolRepository.save(rol));
    }

    @Transactional
    public RolDetalleResponse actualizarPermisos(Long id, Set<Long> idsPermisos) {
        Rol rol = buscarRol(id);
        Set<Permiso> permisos = cargarPermisos(idsPermisos);
        protegerPermisosDelAdministrador(rol, permisos);
        rol.setPermisos(permisos);
        return RolDetalleResponse.from(rol);
    }

    private Rol buscarRol(Long id) {
        return rolRepository.findByIdWithPermisos(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el rol solicitado"));
    }

    private Set<Permiso> cargarPermisos(Set<Long> idsPermisos) {
        if (idsPermisos.isEmpty()) {
            return new HashSet<>();
        }
        List<Permiso> permisos = permisoRepository.findAllById(idsPermisos);
        if (permisos.size() != idsPermisos.size()) {
            throw new RecursoNoEncontradoException("Uno o más permisos no existen");
        }
        return new HashSet<>(permisos);
    }

    private void protegerPermisosDelAdministrador(Rol rol, Set<Permiso> permisos) {
        if (!ROL_ADMINISTRADOR.equalsIgnoreCase(rol.getNombre())) {
            return;
        }
        Set<String> codigos = permisos.stream().map(Permiso::getCodigo).collect(java.util.stream.Collectors.toSet());
        if (!codigos.containsAll(PermisosSeguridad.ADMINISTRADOR_OBLIGATORIOS)) {
            throw new OperacionNoPermitidaException(
                "El rol Administrador debe conservar todos los permisos de seguridad obligatorios"
            );
        }
    }

    private String normalizarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return null;
        }
        return descripcion.strip();
    }
}
