package pe.com.proveperu.sgc.devolucion.domain.model;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.proveperu.sgc.security.domain.model.Usuario;
import pe.com.proveperu.sgc.configuracion.domain.model.MetodoPago;
import pe.com.proveperu.sgc.venta.domain.model.Venta;

@Entity
@Table(name = "devolucion")
@Getter
@Setter
@NoArgsConstructor
public class Devolucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_devolucion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_venta", nullable = false)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private Instant fechaHora;

    @Column(name = "motivo", nullable = false, length = 300)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_solucion", nullable = false, length = 30)
    private TipoSolucionDevolucion tipoSolucion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoDevolucion estado;

    @Column(name = "importe_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal importeTotal;

    @Column(name = "importe_aplicado_saldo", nullable = false, precision = 14, scale = 2)
    private BigDecimal importeAplicadoSaldo;

    @Column(name = "importe_reembolsable", nullable = false, precision = 14, scale = 2)
    private BigDecimal importeReembolsable;

    @Column(name = "importe_reembolsado", nullable = false, precision = 14, scale = 2)
    private BigDecimal importeReembolsado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_resolucion")
    private Usuario usuarioResolucion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_metodo_pago_resolucion")
    private MetodoPago metodoPagoResolucion;

    @Column(name = "fecha_resolucion")
    private Instant fechaResolucion;

    @Column(name = "referencia_resolucion", length = 120)
    private String referenciaResolucion;

    @Column(name = "importe_reemplazo", nullable = false, precision = 14, scale = 2)
    private BigDecimal importeReemplazo = BigDecimal.ZERO.setScale(2);

    @Column(name = "importe_cobrado", nullable = false, precision = 14, scale = 2)
    private BigDecimal importeCobrado = BigDecimal.ZERO.setScale(2);

    @OneToMany(mappedBy = "devolucion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<DetalleDevolucion> detalles = new ArrayList<>();

    @OneToMany(mappedBy = "devolucion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<DetalleCambioDevolucion> detallesCambio = new ArrayList<>();

    @OneToOne(mappedBy = "devolucion", fetch = FetchType.LAZY)
    private ReembolsoDevolucion reembolso;

    public void agregarDetalle(DetalleDevolucion detalle) {
        detalle.setDevolucion(this);
        detalles.add(detalle);
    }

    public void agregarDetalleCambio(DetalleCambioDevolucion detalle) {
        detalle.setDevolucion(this);
        detallesCambio.add(detalle);
    }

    @PrePersist
    void asignarFechaHora() {
        if (fechaHora == null) {
            fechaHora = Instant.now();
        }
    }
}
