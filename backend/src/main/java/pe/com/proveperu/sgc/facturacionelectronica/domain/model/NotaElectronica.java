package pe.com.proveperu.sgc.facturacionelectronica.domain.model;

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
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.proveperu.sgc.comprobante.domain.model.Comprobante;
import pe.com.proveperu.sgc.security.domain.model.Usuario;

@Entity
@Table(name = "nota_electronica")
@Getter
@Setter
@NoArgsConstructor
public class NotaElectronica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nota_electronica")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_comprobante_origen", nullable = false)
    private Comprobante comprobanteOrigen;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoNotaElectronica tipo;

    @Column(name = "serie", nullable = false, length = 4)
    private String serie;

    @Column(name = "numero", nullable = false, length = 8)
    private String numero;

    @Column(name = "codigo_motivo", nullable = false, length = 2)
    private String codigoMotivo;

    @Column(name = "descripcion_motivo", nullable = false, length = 300)
    private String descripcionMotivo;

    @Column(name = "fecha_emision", nullable = false, updatable = false)
    private Instant fechaEmision;

    @Column(name = "subtotal", nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "igv", nullable = false, precision = 14, scale = 2)
    private BigDecimal igv;

    @Column(name = "total", nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "ambiente", nullable = false, length = 20)
    private AmbienteSunat ambiente;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 40)
    private EstadoEnvioSunat estado;

    @Column(name = "nombre_archivo", nullable = false, length = 120)
    private String nombreArchivo;

    @Column(name = "hash_xml", nullable = false, length = 64)
    private String hashXml;

    @Column(name = "xml_firmado", nullable = false)
    private byte[] xmlFirmado;

    @Column(name = "zip_enviado", nullable = false)
    private byte[] zipEnviado;

    @Column(name = "cdr_zip")
    private byte[] cdrZip;

    @Column(name = "codigo_respuesta", length = 20)
    private String codigoRespuesta;

    @Column(name = "descripcion_respuesta", length = 1000)
    private String descripcionRespuesta;

    @Column(name = "observaciones", columnDefinition = "text")
    private String observaciones;

    @Column(name = "error_ultimo", length = 2000)
    private String errorUltimo;

    @Column(name = "intentos", nullable = false)
    private int intentos;

    @Column(name = "fecha_ultimo_intento")
    private Instant fechaUltimoIntento;

    @Column(name = "fecha_respuesta")
    private Instant fechaRespuesta;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    public String getNumeroCompleto() { return serie + "-" + numero; }

    @PrePersist
    void asignarFecha() { if (fechaEmision == null) fechaEmision = Instant.now(); }
}
