package pe.com.proveperu.sgc.caja.domain.model;

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
import pe.com.proveperu.sgc.security.domain.model.Usuario;

@Entity
@Table(name = "sesion_caja")
@Getter
@Setter
@NoArgsConstructor
public class SesionCaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sesion_caja")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_caja", nullable = false)
    private Caja caja;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_apertura", nullable = false)
    private Usuario usuarioApertura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_cierre")
    private Usuario usuarioCierre;

    @Column(name = "fecha_hora_apertura", nullable = false, updatable = false)
    private Instant fechaHoraApertura;

    @Column(name = "saldo_inicial", nullable = false, precision = 14, scale = 2)
    private BigDecimal saldoInicial;

    @Column(name = "fecha_hora_cierre")
    private Instant fechaHoraCierre;

    @Column(name = "saldo_esperado", precision = 14, scale = 2)
    private BigDecimal saldoEsperado;

    @Column(name = "saldo_real", precision = 14, scale = 2)
    private BigDecimal saldoReal;

    @Column(name = "diferencia", precision = 14, scale = 2)
    private BigDecimal diferencia;

    @Column(name = "observacion_cierre", length = 300)
    private String observacionCierre;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoSesionCaja estado = EstadoSesionCaja.ABIERTA;

    @PrePersist
    void asignarFechaHoraApertura() {
        if (fechaHoraApertura == null) {
            fechaHoraApertura = Instant.now();
        }
    }
}
