package pe.com.proveperu.sgc.cotizacion.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.proveperu.sgc.cotizacion.domain.model.Cotizacion;
import pe.com.proveperu.sgc.cotizacion.domain.model.EstadoCotizacion;

public interface CotizacionRepository
    extends JpaRepository<Cotizacion, Long>, JpaSpecificationExecutor<Cotizacion> {

    @EntityGraph(attributePaths = {
        "cliente",
        "usuario",
        "detalles",
        "detalles.producto",
        "detalles.unidadMedida"
    })
    @Query("select distinct c from Cotizacion c where c.id = :id")
    Optional<Cotizacion> findDetalleById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cotizacion c where c.id = :id")
    Optional<Cotizacion> findForUpdate(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update Cotizacion c
           set c.estado = :estadoVencido
         where c.fechaVencimiento < :hoy
           and c.estado in :estadosVigentes
        """)
    int marcarVencidas(
        @Param("hoy") LocalDate hoy,
        @Param("estadoVencido") EstadoCotizacion estadoVencido,
        @Param("estadosVigentes") Collection<EstadoCotizacion> estadosVigentes
    );
}
