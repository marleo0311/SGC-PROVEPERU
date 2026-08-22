package pe.com.proveperu.sgc.pedido.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.proveperu.sgc.pedido.domain.model.Pedido;

public interface PedidoRepository
    extends JpaRepository<Pedido, Long>, JpaSpecificationExecutor<Pedido> {

    boolean existsByCotizacionId(Long idCotizacion);

    @EntityGraph(attributePaths = {
        "cliente",
        "cotizacion",
        "usuario",
        "sede",
        "detalles",
        "detalles.producto",
        "detalles.producto.unidadBase",
        "detalles.unidadMedida"
    })
    @Query("select distinct p from Pedido p where p.id = :id")
    Optional<Pedido> findDetalleById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id")
    Optional<Pedido> findForUpdate(@Param("id") Long id);
}
