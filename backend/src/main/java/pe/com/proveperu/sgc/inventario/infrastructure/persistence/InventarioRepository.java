package pe.com.proveperu.sgc.inventario.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.catalogo.domain.model.Producto;
import pe.com.proveperu.sgc.inventario.domain.model.Inventario;

public interface InventarioRepository extends JpaRepository<Inventario, Long> {

    @EntityGraph(attributePaths = {"sede", "producto", "producto.unidadBase"})
    Optional<Inventario> findBySedeIdAndProductoId(Long idSede, Long idProducto);

    @EntityGraph(attributePaths = {"sede", "producto", "producto.unidadBase"})
    List<Inventario> findAllBySedeIdAndProductoIdIn(Long idSede, Collection<Long> productos);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"sede", "producto", "producto.unidadBase"})
    @Query("select i from Inventario i where i.sede.id = :idSede and i.producto.id = :idProducto")
    Optional<Inventario> findForUpdate(
        @Param("idSede") Long idSede,
        @Param("idProducto") Long idProducto
    );

    @Query(
        value = """
            select p from Producto p
            join fetch p.categoria
            left join fetch p.marca
            join fetch p.unidadBase
            left join Inventario i
              on i.producto.id = p.id and i.sede.id = :idSede
            where p.estado = :estado
              and coalesce(i.stockFisico, 0) - coalesce(i.stockReservado, 0) <= p.stockMinimo
              and (
                  :buscar = ''
                  or lower(p.nombre) like lower(concat('%', :buscar, '%'))
                  or lower(p.codigoInterno) like lower(concat('%', :buscar, '%'))
                  or lower(coalesce(p.codigoBarras, '')) like lower(concat('%', :buscar, '%'))
              )
            """,
        countQuery = """
            select count(p) from Producto p
            left join Inventario i
              on i.producto.id = p.id and i.sede.id = :idSede
            where p.estado = :estado
              and coalesce(i.stockFisico, 0) - coalesce(i.stockReservado, 0) <= p.stockMinimo
              and (
                  :buscar = ''
                  or lower(p.nombre) like lower(concat('%', :buscar, '%'))
                  or lower(p.codigoInterno) like lower(concat('%', :buscar, '%'))
                  or lower(coalesce(p.codigoBarras, '')) like lower(concat('%', :buscar, '%'))
              )
            """
    )
    Page<Producto> buscarProductosConStockBajo(
        @Param("idSede") Long idSede,
        @Param("estado") EstadoCatalogo estado,
        @Param("buscar") String buscar,
        Pageable pageable
    );
}
