package pe.com.proveperu.sgc.cotizacion.domain.model;

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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.proveperu.sgc.cliente.domain.model.Cliente;
import pe.com.proveperu.sgc.security.domain.model.Usuario;

@Entity
@Table(name = "cotizacion")
@Getter
@Setter
@NoArgsConstructor
public class Cotizacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cotizacion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "igv", nullable = false, precision = 14, scale = 2)
    private BigDecimal igv;

    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoCotizacion estado = EstadoCotizacion.PENDIENTE;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private Instant fechaRegistro;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    @OneToMany(mappedBy = "cotizacion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<DetalleCotizacion> detalles = new ArrayList<>();

    public void agregarDetalle(DetalleCotizacion detalle) {
        detalle.setCotizacion(this);
        detalles.add(detalle);
    }

    @PrePersist
    void registrarFechas() {
        Instant ahora = Instant.now();
        if (fechaRegistro == null) {
            fechaRegistro = ahora;
        }
        fechaActualizacion = ahora;
    }

    @PreUpdate
    void actualizarFecha() {
        fechaActualizacion = Instant.now();
    }
}
