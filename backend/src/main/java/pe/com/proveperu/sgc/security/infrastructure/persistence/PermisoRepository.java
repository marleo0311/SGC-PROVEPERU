package pe.com.proveperu.sgc.security.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.proveperu.sgc.security.domain.model.Permiso;

public interface PermisoRepository extends JpaRepository<Permiso, Long> {

    Optional<Permiso> findByCodigo(String codigo);

    List<Permiso> findAllByModuloOrderByCodigoAsc(String modulo);

    List<Permiso> findAllByOrderByModuloAscCodigoAsc();

    boolean existsByCodigo(String codigo);
}
