package pe.com.proveperu.sgc.compra.infrastructure.persistence;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.proveperu.sgc.compra.domain.model.DetalleRecepcionCompra;

public interface DetalleRecepcionCompraRepository
    extends JpaRepository<DetalleRecepcionCompra, Long> {

    @Query("""
        select d.detalleCompra.id as idDetalleCompra,
               sum(d.cantidadRecibida) as cantidadRecibida
        from DetalleRecepcionCompra d
        where d.recepcion.compra.id = :idCompra
        group by d.detalleCompra.id
        """)
    List<CantidadRecibidaPorDetalle> sumarCantidadesPorCompra(
        @Param("idCompra") Long idCompra
    );

    interface CantidadRecibidaPorDetalle {
        Long getIdDetalleCompra();

        BigDecimal getCantidadRecibida();
    }
}
