package pe.com.proveperu.sgc.facturacionelectronica.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.proveperu.sgc.config.OpenApiConfig;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.ResumenDiarioCrearRequest;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.ResumenDiarioSunatResponse;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.ArchivoElectronico;
import pe.com.proveperu.sgc.facturacionelectronica.application.service.ResumenDiarioSunatService;
import pe.com.proveperu.sgc.venta.application.service.PermisosVenta;

@RestController
@RequestMapping("/api/v1/sunat/resumenes-diarios")
@RequiredArgsConstructor
@Validated
@Tag(
    name = "Resúmenes diarios SUNAT",
    description = "Agrupación, envío y seguimiento asíncrono de boletas electrónicas"
)
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ResumenDiarioSunatController {

    private final ResumenDiarioSunatService service;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisosVenta.COMPROBANTES_VER + "')")
    @Operation(summary = "Listar resúmenes diarios, opcionalmente por fecha de emisión")
    public List<ResumenDiarioSunatResponse> listar(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fechaEmision
    ) {
        return service.listar(fechaEmision);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisosVenta.COMPROBANTES_VER + "')")
    @Operation(summary = "Consultar el detalle de un resumen diario")
    public ResumenDiarioSunatResponse detalle(@PathVariable @Positive Long id) {
        return service.detalle(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermisosVenta.SUNAT_RESUMENES_GESTIONAR + "')")
    @Operation(summary = "Generar y firmar los resúmenes de las boletas pendientes de una fecha")
    public List<ResumenDiarioSunatResponse> preparar(
        @Valid @RequestBody ResumenDiarioCrearRequest request
    ) {
        return service.preparar(request.fechaEmision());
    }

    @PostMapping("/{id}/enviar")
    @PreAuthorize("hasAuthority('" + PermisosVenta.SUNAT_RESUMENES_GESTIONAR + "')")
    @Operation(summary = "Enviar un resumen diario y registrar el ticket SUNAT")
    public ResumenDiarioSunatResponse enviar(@PathVariable @Positive Long id) {
        return service.enviar(id);
    }

    @PostMapping("/{id}/consultar")
    @PreAuthorize("hasAuthority('" + PermisosVenta.SUNAT_RESUMENES_GESTIONAR + "')")
    @Operation(summary = "Consultar el ticket y procesar el CDR del resumen diario")
    public ResumenDiarioSunatResponse consultarEstado(@PathVariable @Positive Long id) {
        return service.consultarEstado(id);
    }

    @GetMapping("/{id}/xml")
    @PreAuthorize("hasAuthority('" + PermisosVenta.COMPROBANTES_VER + "')")
    @Operation(summary = "Descargar el XML firmado del resumen diario")
    public ResponseEntity<byte[]> descargarXml(@PathVariable @Positive Long id) {
        return archivo(service.xml(id));
    }

    @GetMapping("/{id}/cdr")
    @PreAuthorize("hasAuthority('" + PermisosVenta.COMPROBANTES_VER + "')")
    @Operation(summary = "Descargar el CDR del resumen diario")
    public ResponseEntity<byte[]> descargarCdr(@PathVariable @Positive Long id) {
        return archivo(service.cdr(id));
    }

    private ResponseEntity<byte[]> archivo(ArchivoElectronico archivo) {
        ContentDisposition disposition = ContentDisposition.attachment()
            .filename(archivo.nombre(), StandardCharsets.UTF_8)
            .build();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .contentType(MediaType.parseMediaType(archivo.contentType()))
            .contentLength(archivo.contenido().length)
            .body(archivo.contenido());
    }
}
