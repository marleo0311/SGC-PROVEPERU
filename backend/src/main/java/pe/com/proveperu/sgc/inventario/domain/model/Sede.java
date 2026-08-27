package pe.com.proveperu.sgc.inventario.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sede")
@Getter
@Setter
@NoArgsConstructor
public class Sede {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sede")
    private Long id;

    @Column(name = "id_empresa", nullable = false)
    private Long idEmpresa;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "direccion", length = 250)
    private String direccion;

    @Column(name = "codigo_establecimiento_sunat", nullable = false, length = 4)
    private String codigoEstablecimientoSunat = "0000";

    @Column(name = "es_sede_facturacion", nullable = false)
    private boolean sedeFacturacion;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;
}
