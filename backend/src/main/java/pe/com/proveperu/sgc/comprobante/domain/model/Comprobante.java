package pe.com.proveperu.sgc.comprobante.domain.model;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.proveperu.sgc.security.domain.model.Usuario;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.EnvioSunat;
import pe.com.proveperu.sgc.venta.domain.model.TipoComprobanteVenta;
import pe.com.proveperu.sgc.venta.domain.model.Venta;

@Entity
@Table(name = "comprobante")
@Getter
@Setter
@NoArgsConstructor
public class Comprobante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comprobante")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_venta", nullable = false, unique = true)
    private Venta venta;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoComprobanteVenta tipo;

    @Column(name = "serie", nullable = false, length = 20)
    private String serie;

    @Column(name = "numero", nullable = false, length = 30)
    private String numero;

    @Column(name = "fecha_emision", nullable = false, updatable = false)
    private Instant fechaEmision;

    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "igv", nullable = false, precision = 14, scale = 2)
    private BigDecimal igv;

    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoComprobante estado = EstadoComprobante.EMITIDO;

    @Column(name = "fecha_anulacion")
    private Instant fechaAnulacion;

    @Column(name = "motivo_anulacion", length = 300)
    private String motivoAnulacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_anulacion")
    private Usuario usuarioAnulacion;

    @OneToOne(mappedBy = "comprobante", fetch = FetchType.LAZY)
    private EnvioSunat envioSunat;

    public String getNumeroCompleto() {
        return serie + "-" + numero;
    }

    @PrePersist
    void asignarFechaEmision() {
        if (fechaEmision == null) {
            fechaEmision = Instant.now();
        }
    }
}
