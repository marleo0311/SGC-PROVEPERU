package pe.com.proveperu.sgc.compra.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import pe.com.proveperu.sgc.catalogo.domain.model.Producto;
import pe.com.proveperu.sgc.catalogo.domain.model.UnidadMedida;

@Entity
@Table(name = "detalle_recepcion_compra")
@Getter
@Setter
@NoArgsConstructor
public class DetalleRecepcionCompra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_recepcion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_recepcion", nullable = false)
    private RecepcionCompra recepcion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_detalle_compra", nullable = false)
    private DetalleCompra detalleCompra;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_unidad_medida", nullable = false)
    private UnidadMedida unidadMedida;

    @Column(name = "cantidad_esperada", nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidadEsperada;

    @Column(name = "cantidad_recibida", nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidadRecibida;

    @Column(name = "cantidad_acumulada", nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidadAcumulada;

    @Column(name = "cantidad_pendiente", nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidadPendiente;

    @Column(name = "conforme", nullable = false)
    private boolean conforme;

    @Column(name = "observacion", length = 250)
    private String observacion;
}
