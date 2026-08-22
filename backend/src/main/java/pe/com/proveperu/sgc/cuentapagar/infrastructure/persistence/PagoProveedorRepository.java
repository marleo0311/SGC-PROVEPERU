package pe.com.proveperu.sgc.cuentapagar.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.proveperu.sgc.cuentapagar.domain.model.PagoProveedor;

public interface PagoProveedorRepository extends JpaRepository<PagoProveedor, Long> {

    @EntityGraph(attributePaths = {"metodoPago", "usuario"})
    List<PagoProveedor> findAllByCuentaPagarIdOrderByFechaHoraDescIdDesc(
        Long idCuentaPagar
    );
}
