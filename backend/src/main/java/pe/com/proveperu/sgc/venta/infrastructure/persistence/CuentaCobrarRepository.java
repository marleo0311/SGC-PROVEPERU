package pe.com.proveperu.sgc.venta.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.proveperu.sgc.venta.domain.model.CuentaCobrar;

public interface CuentaCobrarRepository extends JpaRepository<CuentaCobrar, Long> {

    @EntityGraph(attributePaths = "venta")
    Optional<CuentaCobrar> findByVentaId(Long idVenta);
}
