package pe.com.proveperu.sgc.inventario.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.proveperu.sgc.inventario.domain.model.ConsumoExistenciaPresentacion;

public interface ConsumoExistenciaPresentacionRepository
    extends JpaRepository<ConsumoExistenciaPresentacion, Long> {

    @EntityGraph(attributePaths = {"detalleVenta", "existencia"})
    List<ConsumoExistenciaPresentacion> findAllByDetalleVentaVentaIdOrderByIdAsc(Long idVenta);
}
