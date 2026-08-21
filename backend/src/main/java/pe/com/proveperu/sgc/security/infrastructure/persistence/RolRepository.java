package pe.com.proveperu.sgc.security.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.proveperu.sgc.security.domain.model.Rol;

public interface RolRepository extends JpaRepository<Rol, Long> {

    Optional<Rol> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

    @EntityGraph(attributePaths = "permisos")
    List<Rol> findAllByOrderByNombreAsc();

    @EntityGraph(attributePaths = "permisos")
    @Query("select r from Rol r where r.id = :id")
    Optional<Rol> findByIdWithPermisos(@Param("id") Long id);
}
