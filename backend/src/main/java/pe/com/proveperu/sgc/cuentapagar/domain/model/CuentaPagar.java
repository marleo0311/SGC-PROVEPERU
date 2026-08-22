package pe.com.proveperu.sgc.cuentapagar.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.proveperu.sgc.compra.domain.model.Compra;

@Entity
@Table(name = "cuenta_pagar")
@Getter
@Setter
@NoArgsConstructor
public class CuentaPagar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cuenta_pagar")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_compra", nullable = false, unique = true)
    private Compra compra;

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
    private EstadoCuentaPagar estado;
}
