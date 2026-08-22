package pe.com.proveperu.sgc.transporte.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.proveperu.sgc.transporte.domain.model.Gasto;

public interface GastoRepository
    extends JpaRepository<Gasto, Long>, JpaSpecificationExecutor<Gasto> {

    @EntityGraph(attributePaths = {"transportista", "usuario"})
    List<Gasto> findAllByTransportistaIdOrderByFechaDescIdDesc(Long idTransportista);

    @EntityGraph(attributePaths = {"transportista", "usuario"})
    List<Gasto> findAllByIdCompraOrderByFechaDescIdDesc(Long idCompra);

    @Query("select coalesce(sum(g.importe), 0) from Gasto g where g.idCompra = :idCompra")
    java.math.BigDecimal sumarImportesPorCompra(@Param("idCompra") Long idCompra);
}
