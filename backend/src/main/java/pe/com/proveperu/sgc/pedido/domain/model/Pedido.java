package pe.com.proveperu.sgc.pedido.domain.model;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.proveperu.sgc.cliente.domain.model.Cliente;
import pe.com.proveperu.sgc.cotizacion.domain.model.Cotizacion;
import pe.com.proveperu.sgc.inventario.domain.model.Sede;
import pe.com.proveperu.sgc.security.domain.model.Usuario;

@Entity
@Table(name = "pedido")
@Getter
@Setter
@NoArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pedido")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cotizacion")
    private Cotizacion cotizacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sede", nullable = false)
    private Sede sede;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 20)
    private CanalPedido canal;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private Instant fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoPedido estado = EstadoPedido.RECIBIDO;

    @Column(name = "observacion", length = 300)
    private String observacion;

    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "igv", nullable = false, precision = 14, scale = 2)
    private BigDecimal igv;

    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<DetallePedido> detalles = new ArrayList<>();

    public void agregarDetalle(DetallePedido detalle) {
        detalle.setPedido(this);
        detalles.add(detalle);
    }

    @PrePersist
    void registrarFechas() {
        Instant ahora = Instant.now();
        if (fechaHora == null) {
            fechaHora = ahora;
        }
        fechaActualizacion = ahora;
    }

    @PreUpdate
    void actualizarFecha() {
        fechaActualizacion = Instant.now();
    }
}
