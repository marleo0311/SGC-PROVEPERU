package pe.com.proveperu.sgc.catalogo.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.catalogo.domain.model.PrecioProducto;

public interface PrecioProductoRepository extends JpaRepository<PrecioProducto, Long> {

    List<PrecioProducto> findAllByProductoIdOrderByTipoPrecioAscVigenteDesdeDesc(Long idProducto);

    @Query("""
        select p from PrecioProducto p
        where p.producto.id = :idProducto
          and upper(p.tipoPrecio) = upper(:tipoPrecio)
          and p.estado = :estado
          and (:vigenteHasta is null or p.vigenteDesde <= :vigenteHasta)
          and (p.vigenteHasta is null or p.vigenteHasta >= :vigenteDesde)
        order by p.vigenteDesde asc
        """)
    List<PrecioProducto> buscarSolapados(
        @Param("idProducto") Long idProducto,
        @Param("tipoPrecio") String tipoPrecio,
        @Param("vigenteDesde") LocalDate vigenteDesde,
        @Param("vigenteHasta") LocalDate vigenteHasta,
        @Param("estado") EstadoCatalogo estado
    );

    @Query("""
        select p from PrecioProducto p
        where p.producto.id = :idProducto
          and upper(p.tipoPrecio) = upper(:tipoPrecio)
          and p.estado = :estado
          and p.vigenteDesde <= :fecha
          and (p.vigenteHasta is null or p.vigenteHasta >= :fecha)
        order by p.vigenteDesde desc, p.id desc
        """)
    List<PrecioProducto> buscarVigentes(
        @Param("idProducto") Long idProducto,
        @Param("tipoPrecio") String tipoPrecio,
        @Param("fecha") LocalDate fecha,
        @Param("estado") EstadoCatalogo estado
    );
}
