package pe.com.proveperu.sgc.venta.domain.model;

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
import pe.com.proveperu.sgc.cliente.domain.model.Cliente;
import pe.com.proveperu.sgc.inventario.domain.model.Sede;
import pe.com.proveperu.sgc.pedido.domain.model.Pedido;
import pe.com.proveperu.sgc.security.domain.model.Usuario;

@Entity
@Table(name = "venta")
@Getter
@Setter
@NoArgsConstructor
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_venta")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_vendedor", nullable = false)
    private Usuario vendedor;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido", unique = true)
    private Pedido pedido;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sede", nullable = false)
    private Sede sede;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private Instant fechaHora;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_venta", nullable = false, length = 20)
    private TipoVenta tipoVenta;

    @Enumerated(EnumType.STRING)
    @Column(name = "condicion_pago", nullable = false, length = 20)
    private CondicionPagoVenta condicionPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comprobante", nullable = false, length = 30)
    private TipoComprobanteVenta tipoComprobante = TipoComprobanteVenta.NOTA_VENTA;

    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "igv", nullable = false, precision = 14, scale = 2)
    private BigDecimal igv;

    @Column(name = "descuento_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal descuentoTotal;

    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoVenta estado = EstadoVenta.REGISTRADA;

    @Column(name = "fecha_anulacion")
    private Instant fechaAnulacion;

    @Column(name = "motivo_anulacion", length = 300)
    private String motivoAnulacion;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<DetalleVenta> detalles = new ArrayList<>();

    @OneToOne(mappedBy = "venta", fetch = FetchType.LAZY)
    private CuentaCobrar cuentaCobrar;

    public void agregarDetalle(DetalleVenta detalle) {
        detalle.setVenta(this);
        detalles.add(detalle);
    }

    @PrePersist
    void asignarFechaHora() {
        if (fechaHora == null) {
            fechaHora = Instant.now();
        }
    }
}
