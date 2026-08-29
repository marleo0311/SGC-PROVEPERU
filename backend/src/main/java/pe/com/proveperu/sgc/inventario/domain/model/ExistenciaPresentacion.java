package pe.com.proveperu.sgc.inventario.domain.model;

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
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.proveperu.sgc.catalogo.domain.model.PresentacionProducto;
import pe.com.proveperu.sgc.compra.domain.model.RecepcionCompra;

@Entity
@Table(name = "existencia_presentacion")
@Getter
@Setter
@NoArgsConstructor
public class ExistenciaPresentacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_existencia_presentacion")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_presentacion_producto", nullable = false)
    private PresentacionProducto presentacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sede", nullable = false)
    private Sede sede;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_recepcion_compra")
    private RecepcionCompra recepcionCompra;

    @Column(name = "codigo", length = 80)
    private String codigo;

    @Column(name = "cantidad_inicial_base", nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidadInicialBase;

    @Column(name = "cantidad_disponible_base", nullable = false, precision = 14, scale = 3)
    private BigDecimal cantidadDisponibleBase;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoExistenciaPresentacion estado = EstadoExistenciaPresentacion.CERRADO;

    @Column(name = "fecha_ingreso", nullable = false, updatable = false)
    private Instant fechaIngreso = Instant.now();

    @Column(name = "fecha_apertura")
    private Instant fechaApertura;

    @Column(name = "fecha_agotamiento")
    private Instant fechaAgotamiento;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
