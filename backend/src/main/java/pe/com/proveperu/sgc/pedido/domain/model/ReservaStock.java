package pe.com.proveperu.sgc.pedido.domain.model;

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
import pe.com.proveperu.sgc.catalogo.domain.model.Producto;
import pe.com.proveperu.sgc.inventario.domain.model.Sede;

@Entity
@Table(name = "reserva_stock")
@Getter
@Setter
@NoArgsConstructor
public class ReservaStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reserva")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_detalle_pedido", nullable = false)
    private DetallePedido detallePedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sede", nullable = false)
    private Sede sede;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(name = "cantidad", nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoReservaStock estado = EstadoReservaStock.ACTIVA;

    @Column(name = "fecha_reserva", nullable = false, updatable = false)
    private Instant fechaReserva;

    @Column(name = "fecha_liberacion")
    private Instant fechaLiberacion;

    @PrePersist
    void registrarFecha() {
        if (fechaReserva == null) {
            fechaReserva = Instant.now();
        }
    }
}
