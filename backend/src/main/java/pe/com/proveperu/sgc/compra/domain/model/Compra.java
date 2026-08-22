package pe.com.proveperu.sgc.compra.domain.model;

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
import pe.com.proveperu.sgc.proveedor.domain.model.Proveedor;
import pe.com.proveperu.sgc.security.domain.model.Usuario;

@Entity
@Table(name = "compra")
@Getter
@Setter
@NoArgsConstructor
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_compra")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_proveedor", nullable = false)
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "tipo_comprobante", length = 30)
    private String tipoComprobante;

    @Column(name = "numero_comprobante", length = 60)
    private String numeroComprobante;

    @Enumerated(EnumType.STRING)
    @Column(name = "condicion_pago", nullable = false, length = 20)
    private CondicionPagoCompra condicionPago;

    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "igv", nullable = false, precision = 14, scale = 2)
    private BigDecimal igv = BigDecimal.ZERO;

    @Column(name = "gastos_adicionales", nullable = false, precision = 14, scale = 2)
    private BigDecimal gastosAdicionales = BigDecimal.ZERO;

    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoCompra estado = EstadoCompra.REGISTRADA;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private Instant fechaRegistro;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<DetalleCompra> detalles = new ArrayList<>();

    public void agregarDetalle(DetalleCompra detalle) {
        detalle.setCompra(this);
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
