package pe.com.proveperu.sgc.caja.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.proveperu.sgc.caja.domain.model.Caja;
import pe.com.proveperu.sgc.caja.domain.model.EstadoCaja;

public interface CajaRepository extends JpaRepository<Caja, Long> {

    @EntityGraph(attributePaths = "sede")
    List<Caja> findAllByEstadoOrderBySedeNombreAscNombreAsc(EstadoCaja estado);

    @EntityGraph(attributePaths = "sede")
    @Query("select c from Caja c where c.id = :id")
    Optional<Caja> findDetalleById(@Param("id") Long id);
}
