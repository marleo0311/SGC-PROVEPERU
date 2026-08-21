package pe.com.proveperu.sgc.security.application.service;

import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.security.api.dto.UsuarioActualizarRequest;
import pe.com.proveperu.sgc.security.api.dto.UsuarioCrearRequest;
import pe.com.proveperu.sgc.security.api.dto.UsuarioResponse;
import pe.com.proveperu.sgc.security.application.exception.ConflictoNegocioException;
import pe.com.proveperu.sgc.security.application.exception.OperacionNoPermitidaException;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;
import pe.com.proveperu.sgc.security.domain.model.EstadoRegistro;
import pe.com.proveperu.sgc.security.domain.model.EstadoUsuario;
import pe.com.proveperu.sgc.security.domain.model.Rol;
import pe.com.proveperu.sgc.security.domain.model.Usuario;
import pe.com.proveperu.sgc.security.infrastructure.persistence.RolRepository;
import pe.com.proveperu.sgc.security.infrastructure.persistence.UsuarioRepository;
import pe.com.proveperu.sgc.shared.api.dto.PaginaResponse;

@Service
@RequiredArgsConstructor
public class UsuarioAdminService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PaginaResponse<UsuarioResponse> listar(String buscar, Pageable pageable) {
        String criterio = buscar == null ? "" : buscar.strip();
        Page<UsuarioResponse> pagina = usuarioRepository.buscar(criterio, pageable)
            .map(UsuarioResponse::from);
        return PaginaResponse.from(pagina);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse obtener(Long id) {
        return UsuarioResponse.from(buscarUsuario(id));
    }

    @Transactional
    public UsuarioResponse crear(UsuarioCrearRequest request) {
        String login = normalizarLogin(request.usuarioLogin());
        if (usuarioRepository.existsByUsuarioLoginIgnoreCase(login)) {
            throw new ConflictoNegocioException("El usuario de acceso ya está registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(request.nombreCompleto().strip());
        usuario.setUsuarioLogin(login);
        usuario.setPasswordHash(passwordEncoder.encode(request.password()));
        usuario.setRol(buscarRolActivo(request.idRol()));
        usuario.setEstado(EstadoUsuario.ACTIVO);
        return UsuarioResponse.from(usuarioRepository.save(usuario));
    }

    @Transactional
    public UsuarioResponse actualizar(Long id, UsuarioActualizarRequest request) {
        Usuario usuario = buscarUsuario(id);
        String login = normalizarLogin(request.usuarioLogin());
        if (usuarioRepository.existsByUsuarioLoginIgnoreCaseAndIdNot(login, id)) {
            throw new ConflictoNegocioException("El usuario de acceso ya está registrado");
        }

        usuario.setNombreCompleto(request.nombreCompleto().strip());
        usuario.setUsuarioLogin(login);
        usuario.setRol(buscarRolActivo(request.idRol()));
        return UsuarioResponse.from(usuario);
    }

    @Transactional
    public UsuarioResponse cambiarEstado(Long id, EstadoUsuario estado, Long usuarioActualId) {
        if (id.equals(usuarioActualId) && estado == EstadoUsuario.SUSPENDIDO) {
            throw new OperacionNoPermitidaException("No puedes suspender tu propia cuenta");
        }

        Usuario usuario = buscarUsuario(id);
        usuario.setEstado(estado);
        return UsuarioResponse.from(usuario);
    }

    @Transactional
    public void cambiarPassword(Long id, String password) {
        Usuario usuario = buscarUsuario(id);
        usuario.setPasswordHash(passwordEncoder.encode(password));
    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findByIdWithRol(id)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario solicitado"));
    }

    private Rol buscarRolActivo(Long idRol) {
        Rol rol = rolRepository.findById(idRol)
            .orElseThrow(() -> new RecursoNoEncontradoException("No existe el rol solicitado"));
        if (rol.getEstado() != EstadoRegistro.ACTIVO) {
            throw new OperacionNoPermitidaException("No se puede asignar un rol inactivo");
        }
        return rol;
    }

    private String normalizarLogin(String login) {
        return login.strip().toLowerCase(Locale.ROOT);
    }
}
