package pe.com.proveperu.sgc.security.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.proveperu.sgc.security.domain.model.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    @EntityGraph(attributePaths = {"rol", "rol.permisos"})
    Optional<Usuario> findByUsuarioLoginIgnoreCase(String usuarioLogin);

    boolean existsByUsuarioLoginIgnoreCase(String usuarioLogin);

    boolean existsByUsuarioLoginIgnoreCaseAndIdNot(String usuarioLogin, Long id);

    @EntityGraph(attributePaths = "rol")
    @Query("""
        select u from Usuario u
        where :buscar = ''
           or lower(u.nombreCompleto) like lower(concat('%', :buscar, '%'))
           or lower(u.usuarioLogin) like lower(concat('%', :buscar, '%'))
        """)
    Page<Usuario> buscar(@Param("buscar") String buscar, Pageable pageable);

    @EntityGraph(attributePaths = "rol")
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> findByIdWithRol(@Param("id") Long id);
}
