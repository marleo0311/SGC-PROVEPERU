package pe.com.proveperu.sgc.venta.infrastructure.persistence;

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
import pe.com.proveperu.sgc.venta.domain.model.CuentaCobrar;
import pe.com.proveperu.sgc.venta.domain.model.EstadoCuentaCobrar;

public interface CuentaCobrarRepository
    extends JpaRepository<CuentaCobrar, Long>,
    JpaSpecificationExecutor<CuentaCobrar> {

    @EntityGraph(attributePaths = {
        "venta",
        "venta.cliente",
        "venta.vendedor",
        "venta.pedido",
        "venta.sede",
        "venta.comprobante"
    })
    Optional<CuentaCobrar> findByVentaId(Long idVenta);

    @EntityGraph(attributePaths = {
        "venta",
        "venta.cliente",
        "venta.vendedor",
        "venta.pedido",
        "venta.sede",
        "venta.comprobante"
    })
    @Query("select cc from CuentaCobrar cc where cc.id = :id")
    Optional<CuentaCobrar> findDetalleById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
        "venta",
        "venta.cliente",
        "venta.vendedor",
        "venta.pedido",
        "venta.sede",
        "venta.comprobante"
    })
    @Query("select cc from CuentaCobrar cc where cc.id = :id")
    Optional<CuentaCobrar> findForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
        "venta",
        "venta.cliente",
        "venta.vendedor",
        "venta.pedido",
        "venta.sede",
        "venta.comprobante"
    })
    @Query("select cc from CuentaCobrar cc where cc.venta.id = :idVenta")
    Optional<CuentaCobrar> findByVentaIdForUpdate(
        @Param("idVenta") Long idVenta
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update CuentaCobrar cc
           set cc.estado = :estadoVencido
         where cc.saldoPendiente > 0
           and cc.fechaVencimiento < :hoy
           and cc.estado in :estadosPendientes
        """)
    int marcarVencidas(
        @Param("hoy") LocalDate hoy,
        @Param("estadoVencido") EstadoCuentaCobrar estadoVencido,
        @Param("estadosPendientes") Collection<EstadoCuentaCobrar> estadosPendientes
    );
}
