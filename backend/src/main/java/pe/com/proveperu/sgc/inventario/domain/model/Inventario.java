package pe.com.proveperu.sgc.inventario.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.proveperu.sgc.catalogo.domain.model.Producto;

@Entity
@Table(name = "inventario")
@Getter
@Setter
@NoArgsConstructor
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_inventario")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sede", nullable = false)
    private Sede sede;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(name = "stock_fisico", nullable = false, precision = 14, scale = 3)
    private BigDecimal stockFisico = BigDecimal.ZERO;

    @Column(name = "stock_reservado", nullable = false, precision = 14, scale = 3)
    private BigDecimal stockReservado = BigDecimal.ZERO;

    @Column(name = "stock_minimo", nullable = false, precision = 14, scale = 3)
    private BigDecimal stockMinimo;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @PrePersist
    @PreUpdate
    void actualizarFecha() {
        if (stockMinimo == null) {
            stockMinimo = producto == null || producto.getStockMinimo() == null
                ? BigDecimal.ZERO
                : producto.getStockMinimo();
        }
        fechaActualizacion = Instant.now();
    }

    public BigDecimal getStockDisponible() {
        return stockFisico.subtract(stockReservado);
    }
}
