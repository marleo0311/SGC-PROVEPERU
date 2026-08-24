package pe.com.proveperu.sgc.facturacionelectronica.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.NotaElectronica;

public interface NotaElectronicaRepository extends JpaRepository<NotaElectronica, Long> {

    @EntityGraph(attributePaths = {"comprobanteOrigen", "comprobanteOrigen.venta", "usuario"})
    List<NotaElectronica> findByComprobanteOrigenIdOrderByFechaEmisionDesc(Long idComprobante);

    @EntityGraph(attributePaths = {"comprobanteOrigen", "comprobanteOrigen.venta", "comprobanteOrigen.venta.sede", "usuario"})
    @Query("select n from NotaElectronica n where n.id = :id")
    Optional<NotaElectronica> findDetalleById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from NotaElectronica n where n.id = :id")
    Optional<NotaElectronica> findForUpdateById(@Param("id") Long id);

    @Query(value = "select nextval('nota_credito_numero_seq')", nativeQuery = true)
    Long siguienteCredito();

    @Query(value = "select nextval('nota_debito_numero_seq')", nativeQuery = true)
    Long siguienteDebito();
}
