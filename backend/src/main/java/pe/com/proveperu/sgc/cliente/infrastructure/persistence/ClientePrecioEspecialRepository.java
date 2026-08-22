package pe.com.proveperu.sgc.cliente.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.cliente.domain.model.ClientePrecioEspecial;

public interface ClientePrecioEspecialRepository
    extends JpaRepository<ClientePrecioEspecial, Long> {

    @EntityGraph(attributePaths = "producto")
    List<ClientePrecioEspecial>
        findAllByClienteIdOrderByProductoNombreAscVigenteDesdeDesc(Long idCliente);

    List<ClientePrecioEspecial> findAllByClienteIdAndProductoIdAndEstado(
        Long idCliente,
        Long idProducto,
        EstadoCatalogo estado
    );

    @Query("""
        select p from ClientePrecioEspecial p
        where p.cliente.id = :idCliente
          and p.producto.id = :idProducto
          and p.estado = :estado
          and p.vigenteDesde <= :fecha
          and (p.vigenteHasta is null or p.vigenteHasta >= :fecha)
        order by p.vigenteDesde desc, p.id desc
        """)
    List<ClientePrecioEspecial> buscarVigentes(
        @Param("idCliente") Long idCliente,
        @Param("idProducto") Long idProducto,
        @Param("fecha") LocalDate fecha,
        @Param("estado") EstadoCatalogo estado
    );
}
