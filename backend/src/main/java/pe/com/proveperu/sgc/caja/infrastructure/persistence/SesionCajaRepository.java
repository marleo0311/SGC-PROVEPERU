package pe.com.proveperu.sgc.caja.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.proveperu.sgc.caja.domain.model.EstadoSesionCaja;
import pe.com.proveperu.sgc.caja.domain.model.SesionCaja;

public interface SesionCajaRepository extends JpaRepository<SesionCaja, Long> {

    boolean existsByCajaIdAndEstado(Long idCaja, EstadoSesionCaja estado);

    boolean existsByUsuarioAperturaIdAndEstado(
        Long idUsuario,
        EstadoSesionCaja estado
    );

    @EntityGraph(attributePaths = {
        "caja", "caja.sede", "usuarioApertura", "usuarioCierre"
    })
    Optional<SesionCaja> findByCajaIdAndEstado(
        Long idCaja,
        EstadoSesionCaja estado
    );

    @EntityGraph(attributePaths = {
        "caja", "caja.sede", "usuarioApertura", "usuarioCierre"
    })
    @Query("select s from SesionCaja s where s.id = :id")
    Optional<SesionCaja> findDetalleById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
        "caja", "caja.sede", "usuarioApertura", "usuarioCierre"
    })
    @Query("select s from SesionCaja s where s.id = :id")
    Optional<SesionCaja> findForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {
        "caja", "caja.sede", "usuarioApertura", "usuarioCierre"
    })
    @Query("""
        select s from SesionCaja s
        where lower(s.usuarioApertura.usuarioLogin) = lower(:usuarioLogin)
          and s.estado = :estado
        """)
    Optional<SesionCaja> findActivaForUpdate(
        @Param("usuarioLogin") String usuarioLogin,
        @Param("estado") EstadoSesionCaja estado
    );
}
