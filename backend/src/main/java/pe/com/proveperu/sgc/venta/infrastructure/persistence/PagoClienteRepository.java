package pe.com.proveperu.sgc.venta.infrastructure.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.proveperu.sgc.venta.domain.model.PagoCliente;

public interface PagoClienteRepository extends JpaRepository<PagoCliente, Long> {

    @EntityGraph(attributePaths = {"metodoPago", "usuario", "cuentaCobrar"})
    List<PagoCliente> findAllByVentaIdOrderByFechaHoraDescIdDesc(Long idVenta);

    @EntityGraph(attributePaths = {"metodoPago", "usuario", "venta"})
    List<PagoCliente> findAllByCuentaCobrarIdOrderByFechaHoraDescIdDesc(
        Long idCuentaCobrar
    );
}
