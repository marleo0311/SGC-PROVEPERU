package pe.com.proveperu.sgc.proveedor.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pe.com.proveperu.sgc.proveedor.domain.model.Proveedor;

public interface ProveedorRepository
    extends JpaRepository<Proveedor, Long>, JpaSpecificationExecutor<Proveedor> {

    boolean existsByRuc(String ruc);

    boolean existsByRucAndIdNot(String ruc, Long id);
}
