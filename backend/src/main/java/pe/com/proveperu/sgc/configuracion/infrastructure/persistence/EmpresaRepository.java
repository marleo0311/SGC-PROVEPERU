package pe.com.proveperu.sgc.configuracion.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.proveperu.sgc.configuracion.domain.model.Empresa;

public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
}
