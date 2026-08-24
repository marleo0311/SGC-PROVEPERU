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
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.proveperu.sgc.comprobante.domain.model.Comprobante;

@Entity
@Table(name = "resumen_diario_sunat")
@Getter
@Setter
@NoArgsConstructor
public class ResumenDiarioSunat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resumen_diario_sunat")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "ambiente", nullable = false, length = 20)
    private AmbienteSunat ambiente;

    @Column(name = "fecha_documentos", nullable = false)
    private LocalDate fechaDocumentos;

    @Column(name = "fecha_generacion", nullable = false)
    private LocalDate fechaGeneracion;

    @Column(name = "correlativo", nullable = false)
    private int correlativo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 40)
    private EstadoResumenDiarioSunat estado;

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

    @Column(name = "codigo_estado_ticket", length = 20)
    private String codigoEstadoTicket;

    @Column(name = "codigo_respuesta", length = 20)
    private String codigoRespuesta;

    @Column(name = "descripcion_respuesta", length = 1000)
    private String descripcionRespuesta;

    @Column(name = "observaciones", columnDefinition = "text")
    private String observaciones;

    @Column(name = "error_ultimo", length = 2000)
    private String errorUltimo;

    @Column(name = "intentos_envio", nullable = false)
    private int intentosEnvio;

    @Column(name = "consultas_estado", nullable = false)
    private int consultasEstado;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;

    @Column(name = "fecha_ultimo_intento")
    private Instant fechaUltimoIntento;

    @Column(name = "fecha_ultima_consulta")
    private Instant fechaUltimaConsulta;

    @Column(name = "fecha_respuesta")
    private Instant fechaRespuesta;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "resumen_diario_sunat_item",
        joinColumns = @JoinColumn(name = "id_resumen_diario_sunat"),
        inverseJoinColumns = @JoinColumn(name = "id_comprobante")
    )
    private Set<Comprobante> comprobantes = new LinkedHashSet<>();

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
