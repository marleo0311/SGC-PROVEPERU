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
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.com.proveperu.sgc.comprobante.domain.model.Comprobante;
import pe.com.proveperu.sgc.security.domain.model.Usuario;

@Entity
@Table(name = "comunicacion_baja_sunat")
@Getter
@Setter
@NoArgsConstructor
public class ComunicacionBajaSunat {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comunicacion_baja") private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_comprobante", nullable = false) private Comprobante comprobante;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false) private Usuario usuario;
    @Enumerated(EnumType.STRING) @Column(name = "ambiente", nullable = false, length = 20) private AmbienteSunat ambiente;
    @Column(name = "fecha_documento", nullable = false) private LocalDate fechaDocumento;
    @Column(name = "fecha_generacion", nullable = false) private LocalDate fechaGeneracion;
    @Column(name = "correlativo", nullable = false) private int correlativo;
    @Column(name = "motivo", nullable = false, length = 300) private String motivo;
    @Enumerated(EnumType.STRING) @Column(name = "estado", nullable = false, length = 40) private EstadoResumenDiarioSunat estado;
    @Column(name = "nombre_archivo", nullable = false, length = 120) private String nombreArchivo;
    @Column(name = "hash_xml", nullable = false, length = 64) private String hashXml;
    @Column(name = "xml_firmado", nullable = false) private byte[] xmlFirmado;
    @Column(name = "zip_enviado", nullable = false) private byte[] zipEnviado;
    @Column(name = "cdr_zip") private byte[] cdrZip;
    @Column(name = "ticket", length = 120) private String ticket;
    @Column(name = "codigo_estado_ticket", length = 20) private String codigoEstadoTicket;
    @Column(name = "codigo_respuesta", length = 20) private String codigoRespuesta;
    @Column(name = "descripcion_respuesta", length = 1000) private String descripcionRespuesta;
    @Column(name = "observaciones", columnDefinition = "text") private String observaciones;
    @Column(name = "error_ultimo", length = 2000) private String errorUltimo;
    @Column(name = "intentos_envio", nullable = false) private int intentosEnvio;
    @Column(name = "consultas_estado", nullable = false) private int consultasEstado;
    @Column(name = "fecha_creacion", nullable = false) private Instant fechaCreacion;
    @Column(name = "fecha_ultimo_intento") private Instant fechaUltimoIntento;
    @Column(name = "fecha_ultima_consulta") private Instant fechaUltimaConsulta;
    @Column(name = "fecha_respuesta") private Instant fechaRespuesta;
    @Version @Column(name = "version", nullable = false) private long version;
}
