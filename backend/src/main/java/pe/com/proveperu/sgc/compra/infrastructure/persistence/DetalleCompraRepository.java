package pe.com.proveperu.sgc.compra.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.proveperu.sgc.compra.domain.model.DetalleCompra;

public interface DetalleCompraRepository extends JpaRepository<DetalleCompra, Long> {
}
