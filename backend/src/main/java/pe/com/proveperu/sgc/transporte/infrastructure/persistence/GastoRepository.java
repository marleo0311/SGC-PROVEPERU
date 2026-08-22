package pe.com.proveperu.sgc.transporte.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pe.com.proveperu.sgc.transporte.domain.model.Gasto;

public interface GastoRepository
    extends JpaRepository<Gasto, Long>, JpaSpecificationExecutor<Gasto> {

    @EntityGraph(attributePaths = {"transportista", "usuario"})
    List<Gasto> findAllByTransportistaIdOrderByFechaDescIdDesc(Long idTransportista);
}
