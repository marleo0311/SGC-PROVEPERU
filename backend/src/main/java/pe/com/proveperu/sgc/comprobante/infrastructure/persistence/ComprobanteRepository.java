package pe.com.proveperu.sgc.comprobante.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.proveperu.sgc.comprobante.domain.model.Comprobante;
import pe.com.proveperu.sgc.venta.domain.model.TipoComprobanteVenta;

public interface ComprobanteRepository extends JpaRepository<Comprobante, Long> {

    @EntityGraph(attributePaths = {
        "venta",
        "venta.cliente",
        "venta.vendedor",
        "venta.pedido",
        "venta.sede",
        "venta.detalles",
        "venta.detalles.producto",
        "venta.detalles.producto.unidadBase",
        "venta.detalles.unidadMedida",
        "venta.cuentaCobrar",
        "usuarioAnulacion",
        "envioSunat"
    })
    @Query("select distinct c from Comprobante c where c.id = :id")
    Optional<Comprobante> findDetalleById(@Param("id") Long id);

    @EntityGraph(attributePaths = {
        "venta",
        "venta.cliente",
        "venta.vendedor",
        "venta.pedido",
        "venta.sede",
        "venta.detalles",
        "venta.detalles.producto",
        "venta.detalles.producto.unidadBase",
        "venta.detalles.unidadMedida",
        "venta.cuentaCobrar",
        "usuarioAnulacion",
        "envioSunat"
    })
    @Query("select distinct c from Comprobante c where c.venta.id = :idVenta")
    Optional<Comprobante> findDetalleByVentaId(@Param("idVenta") Long idVenta);

    Optional<Comprobante> findByVentaId(Long idVenta);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Comprobante c where c.venta.id = :idVenta")
    Optional<Comprobante> findForUpdateByVentaId(@Param("idVenta") Long idVenta);

    @Query(value = "select nextval('comprobante_nota_venta_seq')", nativeQuery = true)
    Long siguienteNotaVenta();

    @Query(value = "select nextval('comprobante_boleta_seq')", nativeQuery = true)
    Long siguienteBoleta();

    @Query(value = "select nextval('comprobante_factura_seq')", nativeQuery = true)
    Long siguienteFactura();

    @EntityGraph(attributePaths = {
        "venta",
        "venta.cliente",
        "venta.sede",
        "envioSunat"
    })
    @Query("""
        select distinct c from Comprobante c
        where c.tipo = :tipo
          and c.fechaEmision >= :desde
          and c.fechaEmision < :hasta
          and c.estado <> pe.com.proveperu.sgc.comprobante.domain.model.EstadoComprobante.ANULADO
        order by c.fechaEmision, c.id
        """)
    List<Comprobante> findParaResumenDiario(
        @Param("tipo") TipoComprobanteVenta tipo,
        @Param("desde") Instant desde,
        @Param("hasta") Instant hasta
    );
}
