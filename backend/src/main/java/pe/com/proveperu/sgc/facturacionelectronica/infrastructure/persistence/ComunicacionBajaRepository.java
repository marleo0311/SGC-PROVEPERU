package pe.com.proveperu.sgc.facturacionelectronica.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.AmbienteSunat;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.ComunicacionBajaSunat;

public interface ComunicacionBajaRepository extends JpaRepository<ComunicacionBajaSunat, Long> {
    @EntityGraph(attributePaths = {"comprobante", "comprobante.venta", "comprobante.venta.sede", "usuario"})
    List<ComunicacionBajaSunat> findAllByOrderByFechaCreacionDesc();

    @EntityGraph(attributePaths = {"comprobante", "comprobante.venta", "comprobante.venta.sede", "usuario"})
    Optional<ComunicacionBajaSunat> findByComprobanteIdAndAmbiente(Long idComprobante, AmbienteSunat ambiente);

    @EntityGraph(attributePaths = {"comprobante", "comprobante.venta", "comprobante.venta.sede", "usuario"})
    @Query("select b from ComunicacionBajaSunat b where b.id = :id")
    Optional<ComunicacionBajaSunat> findDetalleById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from ComunicacionBajaSunat b where b.id = :id")
    Optional<ComunicacionBajaSunat> findForUpdateById(@Param("id") Long id);

    @Query(value = "select nextval('comunicacion_baja_correlativo_seq')", nativeQuery = true)
    Long siguienteCorrelativo();
}
