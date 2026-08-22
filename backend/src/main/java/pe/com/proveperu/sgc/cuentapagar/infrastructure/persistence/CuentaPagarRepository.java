package pe.com.proveperu.sgc.cuentapagar.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.proveperu.sgc.cuentapagar.domain.model.CuentaPagar;
import pe.com.proveperu.sgc.cuentapagar.domain.model.EstadoCuentaPagar;

public interface CuentaPagarRepository
    extends JpaRepository<CuentaPagar, Long>, JpaSpecificationExecutor<CuentaPagar> {

    Optional<CuentaPagar> findByCompraId(Long idCompra);

    @EntityGraph(attributePaths = {"compra", "compra.proveedor"})
    @Query("select cp from CuentaPagar cp where cp.id = :id")
    Optional<CuentaPagar> findDetalleById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"compra", "compra.proveedor"})
    @Query("select cp from CuentaPagar cp where cp.id = :id")
    Optional<CuentaPagar> findForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"compra", "compra.proveedor"})
    List<CuentaPagar> findAllByCompraIdIn(Collection<Long> idsCompra);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update CuentaPagar cp
           set cp.estado = :estadoVencido
         where cp.saldoPendiente > 0
           and cp.fechaVencimiento < :hoy
           and cp.estado in :estadosPendientes
        """)
    int marcarVencidas(
        @Param("hoy") LocalDate hoy,
        @Param("estadoVencido") EstadoCuentaPagar estadoVencido,
        @Param("estadosPendientes") Collection<EstadoCuentaPagar> estadosPendientes
    );
}
