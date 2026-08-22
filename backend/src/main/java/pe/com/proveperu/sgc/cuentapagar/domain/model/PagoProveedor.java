package pe.com.proveperu.sgc.cuentapagar.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import pe.com.proveperu.sgc.configuracion.domain.model.MetodoPago;
import pe.com.proveperu.sgc.security.domain.model.Usuario;

@Entity
@Table(name = "pago_proveedor")
@Getter
@Setter
@NoArgsConstructor
public class PagoProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago_proveedor")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cuenta_pagar", nullable = false)
    private CuentaPagar cuentaPagar;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_metodo_pago", nullable = false)
    private MetodoPago metodoPago;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "monto", nullable = false, precision = 14, scale = 2)
    private BigDecimal monto;

    @Column(name = "referencia", length = 120)
    private String referencia;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private Instant fechaHora;

    @PrePersist
    void asignarFechaHora() {
        if (fechaHora == null) {
            fechaHora = Instant.now();
        }
    }
}
