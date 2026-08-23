package pe.com.proveperu.sgc.facturacionelectronica.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.EnvioSunat;

public interface EnvioSunatRepository extends JpaRepository<EnvioSunat, Long> {

    Optional<EnvioSunat> findByComprobanteId(Long idComprobante);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EnvioSunat e where e.comprobante.id = :idComprobante")
    Optional<EnvioSunat> findForUpdateByComprobanteId(
        @Param("idComprobante") Long idComprobante
    );
}
