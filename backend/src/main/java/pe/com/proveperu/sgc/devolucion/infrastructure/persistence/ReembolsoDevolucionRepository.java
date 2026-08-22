package pe.com.proveperu.sgc.devolucion.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.proveperu.sgc.devolucion.domain.model.ReembolsoDevolucion;

public interface ReembolsoDevolucionRepository
    extends JpaRepository<ReembolsoDevolucion, Long> {

    boolean existsByDevolucionId(Long idDevolucion);

    @EntityGraph(attributePaths = {"metodoPago", "usuario"})
    Optional<ReembolsoDevolucion> findByDevolucionId(Long idDevolucion);
}
