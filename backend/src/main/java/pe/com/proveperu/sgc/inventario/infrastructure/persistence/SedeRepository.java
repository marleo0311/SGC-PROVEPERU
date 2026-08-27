package pe.com.proveperu.sgc.inventario.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.proveperu.sgc.inventario.domain.model.Sede;

public interface SedeRepository extends JpaRepository<Sede, Long> {

    Optional<Sede> findFirstByEstadoIgnoreCaseOrderByIdAsc(String estado);

    Optional<Sede> findFirstBySedeFacturacionTrueAndEstadoIgnoreCaseOrderByIdAsc(String estado);

    List<Sede> findAllByEstadoIgnoreCaseOrderByNombreAsc(String estado);
}
