package pe.com.proveperu.sgc.compra.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.proveperu.sgc.compra.domain.model.Compra;

public interface CompraRepository
    extends JpaRepository<Compra, Long>, JpaSpecificationExecutor<Compra> {

    @EntityGraph(attributePaths = {
        "proveedor",
        "usuario",
        "detalles",
        "detalles.producto",
        "detalles.unidadMedida"
    })
    @Query("select distinct c from Compra c where c.id = :id")
    Optional<Compra> findDetalleById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Compra c where c.id = :id")
    Optional<Compra> findForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"proveedor", "usuario"})
    List<Compra> findAllByProveedorIdOrderByFechaDescIdDesc(Long idProveedor);

    boolean existsByProveedorIdAndTipoComprobanteIgnoreCaseAndNumeroComprobanteIgnoreCase(
        Long idProveedor,
        String tipoComprobante,
        String numeroComprobante
    );

    boolean existsByProveedorIdAndTipoComprobanteIgnoreCaseAndNumeroComprobanteIgnoreCaseAndIdNot(
        Long idProveedor,
        String tipoComprobante,
        String numeroComprobante,
        Long id
    );
}
