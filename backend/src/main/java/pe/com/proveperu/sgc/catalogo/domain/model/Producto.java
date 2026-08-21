package pe.com.proveperu.sgc.catalogo.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "producto")
@Getter
@Setter
@NoArgsConstructor
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_producto")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_marca")
    private Marca marca;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_unidad_base", nullable = false)
    private UnidadMedida unidadBase;

    @Column(name = "codigo_interno", nullable = false, length = 60)
    private String codigoInterno;

    @Column(name = "codigo_barras", length = 80)
    private String codigoBarras;

    @Column(name = "nombre", nullable = false, length = 180)
    private String nombre;

    @Column(name = "descripcion", length = 300)
    private String descripcion;

    @Column(name = "stock_minimo", nullable = false, precision = 14, scale = 3)
    private BigDecimal stockMinimo = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoCatalogo estado = EstadoCatalogo.ACTIVO;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private Instant fechaRegistro;

    @PrePersist
    void asignarFechaRegistro() {
        if (fechaRegistro == null) {
            fechaRegistro = Instant.now();
        }
    }
}
