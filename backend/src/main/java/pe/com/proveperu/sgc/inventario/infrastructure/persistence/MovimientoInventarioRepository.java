package pe.com.proveperu.sgc.inventario.infrastructure.persistence;

import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.proveperu.sgc.inventario.domain.model.MovimientoInventario;
import pe.com.proveperu.sgc.inventario.domain.model.TipoMovimientoInventario;

public interface MovimientoInventarioRepository
    extends JpaRepository<MovimientoInventario, Long> {

    @EntityGraph(attributePaths = {
        "sede",
        "producto",
        "producto.unidadBase",
        "usuario",
        "unidadMedida"
    })
    @Query("""
        select m from MovimientoInventario m
        where m.sede.id = :idSede
          and (:idProducto = 0 or m.producto.id = :idProducto)
          and m.fechaHora >= :desde
          and m.fechaHora < :hasta
        """)
    Page<MovimientoInventario> buscar(
        @Param("idSede") Long idSede,
        @Param("idProducto") Long idProducto,
        @Param("desde") Instant desde,
        @Param("hasta") Instant hasta,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {
        "sede",
        "producto",
        "producto.unidadBase",
        "usuario",
        "unidadMedida"
    })
    @Query("""
        select m from MovimientoInventario m
        where m.sede.id = :idSede
          and (:idProducto = 0 or m.producto.id = :idProducto)
          and m.tipoMovimiento = :tipo
          and m.fechaHora >= :desde
          and m.fechaHora < :hasta
        """)
    Page<MovimientoInventario> buscarPorTipo(
        @Param("idSede") Long idSede,
        @Param("idProducto") Long idProducto,
        @Param("tipo") TipoMovimientoInventario tipo,
        @Param("desde") Instant desde,
        @Param("hasta") Instant hasta,
        Pageable pageable
    );
}
