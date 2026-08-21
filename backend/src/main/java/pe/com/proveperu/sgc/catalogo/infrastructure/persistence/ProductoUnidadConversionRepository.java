package pe.com.proveperu.sgc.catalogo.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.catalogo.domain.model.ProductoUnidadConversion;

public interface ProductoUnidadConversionRepository
    extends JpaRepository<ProductoUnidadConversion, Long> {

    @EntityGraph(attributePaths = {"unidadOrigen", "unidadDestino"})
    List<ProductoUnidadConversion>
        findAllByProductoIdOrderByUnidadOrigenNombreAscUnidadDestinoNombreAsc(Long idProducto);

    boolean existsByProductoIdAndUnidadOrigenIdAndUnidadDestinoId(
        Long idProducto,
        Long idUnidadOrigen,
        Long idUnidadDestino
    );

    Optional<ProductoUnidadConversion>
        findByProductoIdAndUnidadOrigenIdAndUnidadDestinoIdAndEstado(
            Long idProducto,
            Long idUnidadOrigen,
            Long idUnidadDestino,
            EstadoCatalogo estado
        );
}
