package pe.com.proveperu.sgc.inventario.domain.model;

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
import pe.com.proveperu.sgc.venta.domain.model.DetalleVenta;

@Entity
@Table(name = "consumo_existencia_presentacion")
@Getter
@Setter
@NoArgsConstructor
public class ConsumoExistenciaPresentacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_consumo_existencia")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_detalle_venta", nullable = false)
    private DetalleVenta detalleVenta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_existencia_presentacion", nullable = false)
    private ExistenciaPresentacion existencia;

    @Column(name = "cantidad_base", nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidadBase;
}
