package pe.com.proveperu.sgc.pedido.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.proveperu.sgc.pedido.domain.model.EstadoReservaStock;
import pe.com.proveperu.sgc.pedido.domain.model.ReservaStock;

public interface ReservaStockRepository extends JpaRepository<ReservaStock, Long> {

    @EntityGraph(attributePaths = {
        "detallePedido",
        "sede",
        "producto",
        "producto.unidadBase"
    })
    List<ReservaStock> findAllByPedidoIdOrderByIdAsc(Long idPedido);

    @EntityGraph(attributePaths = {
        "detallePedido",
        "sede",
        "producto",
        "producto.unidadBase"
    })
    List<ReservaStock> findAllByPedidoIdAndEstadoOrderByProductoIdAsc(
        Long idPedido,
        EstadoReservaStock estado
    );
}
