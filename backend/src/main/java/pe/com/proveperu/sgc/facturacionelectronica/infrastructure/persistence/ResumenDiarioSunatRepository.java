package pe.com.proveperu.sgc.facturacionelectronica.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.AmbienteSunat;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.ResumenDiarioSunat;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.EstadoResumenDiarioSunat;

public interface ResumenDiarioSunatRepository extends JpaRepository<ResumenDiarioSunat, Long> {

    @EntityGraph(attributePaths = {"comprobantes"})
    List<ResumenDiarioSunat> findByEstadoInOrderByFechaCreacionAsc(
        Set<EstadoResumenDiarioSunat> estados
    );

    @EntityGraph(attributePaths = {"comprobantes"})
    List<ResumenDiarioSunat> findByFechaDocumentosOrderByCorrelativoDesc(LocalDate fechaDocumentos);

    @EntityGraph(attributePaths = {"comprobantes"})
    List<ResumenDiarioSunat> findAllByOrderByFechaDocumentosDescCorrelativoDesc();

    @EntityGraph(attributePaths = {"comprobantes"})
    @Query("select distinct r from ResumenDiarioSunat r where r.id = :id")
    Optional<ResumenDiarioSunat> findDetalleById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ResumenDiarioSunat r where r.id = :id")
    Optional<ResumenDiarioSunat> findForUpdateById(@Param("id") Long id);

    @Query("""
        select c.id
        from ResumenDiarioSunat r join r.comprobantes c
        where r.ambiente = :ambiente and c.id in :ids
        """)
    Set<Long> findComprobantesIncluidos(
        @Param("ambiente") AmbienteSunat ambiente,
        @Param("ids") Set<Long> ids
    );
}
