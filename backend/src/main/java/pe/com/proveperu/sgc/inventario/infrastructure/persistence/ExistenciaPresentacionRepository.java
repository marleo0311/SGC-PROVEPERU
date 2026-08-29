package pe.com.proveperu.sgc.inventario.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.proveperu.sgc.inventario.domain.model.EstadoExistenciaPresentacion;
import pe.com.proveperu.sgc.inventario.domain.model.ExistenciaPresentacion;

public interface ExistenciaPresentacionRepository
    extends JpaRepository<ExistenciaPresentacion, Long> {

    @EntityGraph(attributePaths = {
        "sede", "presentacion", "presentacion.producto",
        "presentacion.producto.unidadBase", "presentacion.unidadMedida"
    })
    List<ExistenciaPresentacion> findAllBySede_IdAndPresentacion_Producto_IdAndEstadoInOrderByFechaIngresoAscIdAsc(
        Long idSede,
        Long idProducto,
        Collection<EstadoExistenciaPresentacion> estados
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
        "sede", "presentacion", "presentacion.producto",
        "presentacion.producto.unidadBase", "presentacion.unidadMedida"
    })
    @Query("select e from ExistenciaPresentacion e where e.id = :id")
    Optional<ExistenciaPresentacion> findForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
        "sede", "presentacion", "presentacion.producto",
        "presentacion.producto.unidadBase", "presentacion.unidadMedida"
    })
    @Query("""
        select e from ExistenciaPresentacion e
        where e.sede.id = :idSede
          and e.presentacion.producto.id = :idProducto
          and e.estado in :estados
        order by e.fechaIngreso asc, e.id asc
        """)
    List<ExistenciaPresentacion> findAllForUpdate(
        @Param("idSede") Long idSede,
        @Param("idProducto") Long idProducto,
        @Param("estados") Collection<EstadoExistenciaPresentacion> estados
    );
}
