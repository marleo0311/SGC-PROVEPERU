package pe.com.proveperu.sgc.devolucion.infrastructure.persistence;

import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.proveperu.sgc.devolucion.domain.model.DetalleDevolucion;

public interface DetalleDevolucionRepository
    extends JpaRepository<DetalleDevolucion, Long> {

    @Query("""
        select coalesce(sum(dd.cantidad), 0)
        from DetalleDevolucion dd
        where dd.detalleVenta.id = :idDetalleVenta
        """)
    BigDecimal sumarCantidadDevuelta(
        @Param("idDetalleVenta") Long idDetalleVenta
    );

    @Query("""
        select coalesce(sum(dd.cantidadBase), 0)
        from DetalleDevolucion dd
        where dd.detalleVenta.id = :idDetalleVenta
        """)
    BigDecimal sumarCantidadBaseDevuelta(
        @Param("idDetalleVenta") Long idDetalleVenta
    );

    @Query("""
        select coalesce(sum(dd.importeDevolucion), 0)
        from DetalleDevolucion dd
        where dd.detalleVenta.id = :idDetalleVenta
        """)
    BigDecimal sumarImporteDevuelto(
        @Param("idDetalleVenta") Long idDetalleVenta
    );
}
