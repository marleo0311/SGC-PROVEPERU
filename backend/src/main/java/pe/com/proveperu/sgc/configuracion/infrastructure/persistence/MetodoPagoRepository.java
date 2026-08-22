package pe.com.proveperu.sgc.configuracion.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.proveperu.sgc.configuracion.domain.model.MetodoPago;

public interface MetodoPagoRepository extends JpaRepository<MetodoPago, Long> {

    List<MetodoPago> findAllByEstadoIgnoreCaseOrderByNombreAsc(String estado);

    Optional<MetodoPago> findByCodigoIgnoreCase(String codigo);
}
