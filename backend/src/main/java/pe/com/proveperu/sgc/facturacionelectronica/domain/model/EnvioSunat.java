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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.proveperu.sgc.comprobante.domain.model.Comprobante;

@Entity
@Table(name = "envio_sunat")
@Getter
@Setter
@NoArgsConstructor
public class EnvioSunat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_envio_sunat")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_comprobante", nullable = false, unique = true)
    private Comprobante comprobante;

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

    @Column(name = "ticket", length = 120)
    private String ticket;

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

    @Column(name = "fecha_generacion", nullable = false)
    private Instant fechaGeneracion;

    @Column(name = "fecha_ultimo_intento")
    private Instant fechaUltimoIntento;

    @Column(name = "fecha_respuesta")
    private Instant fechaRespuesta;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
