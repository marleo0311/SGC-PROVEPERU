package pe.com.proveperu.sgc.venta.domain.model;

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
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.proveperu.sgc.cliente.domain.model.Cliente;
import pe.com.proveperu.sgc.security.domain.model.Usuario;

@Entity
@Table(name = "cuenta_cobrar")
@Getter
@Setter
@NoArgsConstructor
public class CuentaCobrar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cuenta_cobrar")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_venta", unique = true)
    private Venta venta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen", nullable = false, length = 30)
    private OrigenCuentaCobrar origen;

    @Column(name = "fecha_origen", nullable = false)
    private LocalDate fechaOrigen;

    @Column(name = "documento_referencia", length = 120)
    private String documentoReferencia;

    @Column(name = "observacion", length = 500)
    private String observacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario_creacion", nullable = false)
    private Usuario usuarioCreacion;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private Instant fechaRegistro;

    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Column(name = "importe_pagado", nullable = false, precision = 14, scale = 2)
    private BigDecimal importePagado;

    @Column(name = "saldo_pendiente", nullable = false, precision = 14, scale = 2)
    private BigDecimal saldoPendiente;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoCuentaCobrar estado;

    @PrePersist
    void asignarFechaRegistro() {
        if (fechaRegistro == null) {
            fechaRegistro = Instant.now();
        }
    }
}
