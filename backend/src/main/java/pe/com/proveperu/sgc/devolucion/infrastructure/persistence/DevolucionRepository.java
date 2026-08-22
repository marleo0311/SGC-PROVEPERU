package pe.com.proveperu.sgc.devolucion.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.proveperu.sgc.devolucion.domain.model.Devolucion;

public interface DevolucionRepository
    extends JpaRepository<Devolucion, Long>,
        JpaSpecificationExecutor<Devolucion> {

    @Override
    @EntityGraph(attributePaths = {"venta", "venta.cliente", "usuario"})
    Page<Devolucion> findAll(
        Specification<Devolucion> specification,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {
        "venta",
        "venta.cliente",
        "venta.vendedor",
        "venta.sede",
        "usuario",
        "detalles",
        "detalles.detalleVenta",
        "detalles.producto",
        "detalles.unidadMedida",
        "reembolso",
        "reembolso.metodoPago",
        "reembolso.usuario"
    })
    @Query("select distinct d from Devolucion d where d.id = :id")
    Optional<Devolucion> findDetalleById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Devolucion d where d.id = :id")
    Optional<Devolucion> findForUpdate(@Param("id") Long id);
}
