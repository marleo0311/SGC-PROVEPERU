package pe.com.proveperu.sgc.security.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.proveperu.sgc.security.domain.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @EntityGraph(attributePaths = {"rol", "rol.permisos"})
    Optional<Usuario> findByUsuarioLoginIgnoreCase(String usuarioLogin);

    boolean existsByUsuarioLoginIgnoreCase(String usuarioLogin);
}
