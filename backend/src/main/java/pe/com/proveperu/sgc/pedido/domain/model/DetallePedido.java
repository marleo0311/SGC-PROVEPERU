package pe.com.proveperu.sgc.pedido.domain.model;

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
@Table(name = "detalle_pedido")
@Getter
@Setter
@NoArgsConstructor
public class DetallePedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_detalle_pedido")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_unidad_medida", nullable = false)
    private UnidadMedida unidadMedida;

    @Column(name = "cantidad", nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidad;

    @Column(name = "cantidad_base", nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidadBase;

    @Column(name = "precio_unitario", nullable = false, precision = 14, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "descuento", nullable = false, precision = 14, scale = 2)
    private BigDecimal descuento;

    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;
}
