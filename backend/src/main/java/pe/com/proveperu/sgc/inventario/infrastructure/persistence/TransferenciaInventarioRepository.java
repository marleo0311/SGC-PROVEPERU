package pe.com.proveperu.sgc.inventario.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.proveperu.sgc.inventario.domain.model.TransferenciaInventario;

public interface TransferenciaInventarioRepository
    extends JpaRepository<TransferenciaInventario, Long> {
}
