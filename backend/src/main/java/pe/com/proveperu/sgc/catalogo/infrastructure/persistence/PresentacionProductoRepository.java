package pe.com.proveperu.sgc.catalogo.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.catalogo.domain.model.PresentacionProducto;

public interface PresentacionProductoRepository
    extends JpaRepository<PresentacionProducto, Long> {

    @EntityGraph(attributePaths = {"producto", "producto.unidadBase", "unidadMedida"})
    List<PresentacionProducto> findAllByProductoIdOrderByNombreAsc(Long idProducto);

    @EntityGraph(attributePaths = {"producto", "producto.unidadBase", "unidadMedida"})
    Optional<PresentacionProducto> findByIdAndProductoId(Long id, Long idProducto);

    @EntityGraph(attributePaths = {"producto", "producto.unidadBase", "unidadMedida"})
    Optional<PresentacionProducto> findByProductoIdAndUnidadMedidaIdAndEstado(
        Long idProducto,
        Long idUnidadMedida,
        EstadoCatalogo estado
    );

    boolean existsByProductoIdAndUnidadMedidaIdAndIdNot(
        Long idProducto,
        Long idUnidadMedida,
        Long id
    );

    boolean existsByProductoIdAndUnidadMedidaId(Long idProducto, Long idUnidadMedida);
}
