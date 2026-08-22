package pe.com.proveperu.sgc.transporte.domain.model;

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
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.proveperu.sgc.security.domain.model.Usuario;

@Entity
@Table(name = "gasto")
@Getter
@Setter
@NoArgsConstructor
public class Gasto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_gasto")
    private Long id;

    @Column(name = "id_compra")
    private Long idCompra;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_transportista")
    private Transportista transportista;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_gasto", nullable = false, length = 50)
    private TipoGasto tipoGasto;

    @Column(name = "descripcion", length = 250)
    private String descripcion;

    @Column(name = "importe", nullable = false, precision = 14, scale = 2)
    private BigDecimal importe;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    @Column(name = "numero_comprobante", length = 60)
    private String numeroComprobante;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private Instant fechaRegistro;

    @PrePersist
    void asignarFechaRegistro() {
        if (fechaRegistro == null) {
            fechaRegistro = Instant.now();
        }
    }
}
