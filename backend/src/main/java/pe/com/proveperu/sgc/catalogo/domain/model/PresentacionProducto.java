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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "presentacion_producto")
@Getter
@Setter
@NoArgsConstructor
public class PresentacionProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_presentacion_producto")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_unidad_medida", nullable = false)
    private UnidadMedida unidadMedida;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "contenido_variable", nullable = false)
    private boolean contenidoVariable;

    @Column(name = "contenido_base_predeterminado", precision = 14, scale = 3)
    private BigDecimal contenidoBasePredeterminado;

    @Column(name = "precio_minorista", precision = 14, scale = 2)
    private BigDecimal precioMinorista;

    @Column(name = "precio_mayorista", precision = 14, scale = 2)
    private BigDecimal precioMayorista;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoCatalogo estado = EstadoCatalogo.ACTIVO;
}
