package pe.com.proveperu.sgc.catalogo.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.catalogo.domain.model.Producto;

public interface ProductoRepository extends JpaRepository<Producto, Long> {

    @EntityGraph(attributePaths = {"categoria", "marca", "unidadBase"})
    @Query("""
        select p from Producto p
        where (:estado is null or p.estado = :estado)
          and (
              :buscar = ''
              or lower(p.nombre) like lower(concat('%', :buscar, '%'))
              or lower(p.codigoInterno) like lower(concat('%', :buscar, '%'))
              or lower(coalesce(p.codigoBarras, '')) like lower(concat('%', :buscar, '%'))
          )
        """)
    Page<Producto> buscar(
        @Param("buscar") String buscar,
        @Param("estado") EstadoCatalogo estado,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"categoria", "marca", "unidadBase"})
    @Query("select p from Producto p where p.id = :id")
    Optional<Producto> findByIdWithReferencias(@Param("id") Long id);

    boolean existsByCodigoInternoIgnoreCase(String codigoInterno);

    boolean existsByCodigoInternoIgnoreCaseAndIdNot(String codigoInterno, Long id);

    boolean existsByCodigoBarrasIgnoreCase(String codigoBarras);

    boolean existsByCodigoBarrasIgnoreCaseAndIdNot(String codigoBarras, Long id);
}
