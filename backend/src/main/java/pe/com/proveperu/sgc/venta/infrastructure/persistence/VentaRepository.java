package pe.com.proveperu.sgc.venta.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.proveperu.sgc.venta.domain.model.Venta;

public interface VentaRepository
    extends JpaRepository<Venta, Long>, JpaSpecificationExecutor<Venta> {

    boolean existsByPedidoId(Long idPedido);

    @EntityGraph(attributePaths = {
        "cliente",
        "vendedor",
        "pedido",
        "sede",
        "detalles",
        "detalles.producto",
        "detalles.producto.unidadBase",
        "detalles.unidadMedida",
        "detalles.existenciaPresentacion",
        "cuentaCobrar",
        "comprobante"
    })
    @Query("select distinct v from Venta v where v.id = :id")
    Optional<Venta> findDetalleById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Venta v where v.id = :id")
    Optional<Venta> findForUpdate(@Param("id") Long id);
}
