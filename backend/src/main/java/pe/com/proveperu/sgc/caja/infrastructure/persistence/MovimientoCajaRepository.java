package pe.com.proveperu.sgc.caja.infrastructure.persistence;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pe.com.proveperu.sgc.caja.domain.model.MovimientoCaja;

public interface MovimientoCajaRepository
    extends JpaRepository<MovimientoCaja, Long>,
        JpaSpecificationExecutor<MovimientoCaja> {

    @Override
    @EntityGraph(attributePaths = {
        "sesion", "metodoPago", "usuario", "venta", "vendedor"
    })
    Page<MovimientoCaja> findAll(
        Specification<MovimientoCaja> specification,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {
        "sesion", "metodoPago", "usuario", "venta", "vendedor"
    })
    List<MovimientoCaja> findAllBySesionIdOrderByFechaHoraAscIdAsc(Long idSesion);
}
