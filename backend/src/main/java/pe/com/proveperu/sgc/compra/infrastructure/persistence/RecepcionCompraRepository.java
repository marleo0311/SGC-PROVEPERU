package pe.com.proveperu.sgc.compra.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.proveperu.sgc.compra.domain.model.RecepcionCompra;

public interface RecepcionCompraRepository extends JpaRepository<RecepcionCompra, Long> {

    @EntityGraph(attributePaths = {
        "compra",
        "sede",
        "usuario",
        "detalles",
        "detalles.detalleCompra",
        "detalles.producto",
        "detalles.unidadMedida"
    })
    @Query("""
        select distinct r from RecepcionCompra r
        where r.compra.id = :idCompra
        order by r.fechaHora desc, r.id desc
        """)
    List<RecepcionCompra> findAllByCompraIdOrderByFechaHoraDescIdDesc(
        @Param("idCompra") Long idCompra
    );
}
