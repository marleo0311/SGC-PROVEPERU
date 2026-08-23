package pe.com.proveperu.sgc.facturacionelectronica.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.proveperu.sgc.config.OpenApiConfig;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.ConfiguracionSunatResponse;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.EnvioSunatResponse;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.ArchivoElectronico;
import pe.com.proveperu.sgc.facturacionelectronica.application.service.FacturacionElectronicaService;
import pe.com.proveperu.sgc.venta.application.service.PermisosVenta;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Tag(
    name = "Facturación electrónica SUNAT",
    description = "Generación, firma, envío y seguimiento de comprobantes electrónicos"
)
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class FacturacionElectronicaController {

    private final FacturacionElectronicaService service;

    @GetMapping("/sunat/configuracion")
    @PreAuthorize("hasAuthority('" + PermisosVenta.COMPROBANTES_VER + "')")
    @Operation(summary = "Consultar el estado no sensible de la configuración SUNAT")
    public ConfiguracionSunatResponse configuracion() {
        return service.configuracion();
    }

    @GetMapping("/comprobantes/{id}/sunat")
    @PreAuthorize("hasAuthority('" + PermisosVenta.COMPROBANTES_VER + "')")
    @Operation(summary = "Consultar el estado de envío electrónico de un comprobante")
    public ResponseEntity<EnvioSunatResponse> consultar(
        @PathVariable @Positive Long id
    ) {
        return service.consultar(id)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/comprobantes/{id}/sunat/preparar")
    @PreAuthorize("hasAuthority('" + PermisosVenta.SUNAT_ENVIAR + "')")
    @Operation(summary = "Generar y firmar el XML UBL 2.1 sin enviarlo")
    public EnvioSunatResponse preparar(@PathVariable @Positive Long id) {
        return service.preparar(id);
    }

    @PostMapping("/comprobantes/{id}/sunat/enviar")
    @PreAuthorize("hasAuthority('" + PermisosVenta.SUNAT_ENVIAR + "')")
    @Operation(summary = "Generar, firmar y enviar el comprobante al receptor configurado")
    public EnvioSunatResponse enviar(@PathVariable @Positive Long id) {
        return service.enviar(id);
    }

    @GetMapping("/comprobantes/{id}/sunat/xml")
    @PreAuthorize("hasAuthority('" + PermisosVenta.COMPROBANTES_VER + "')")
    @Operation(summary = "Descargar el XML UBL firmado")
    public ResponseEntity<byte[]> descargarXml(@PathVariable @Positive Long id) {
        return archivo(service.xml(id));
    }

    @GetMapping("/comprobantes/{id}/sunat/cdr")
    @PreAuthorize("hasAuthority('" + PermisosVenta.COMPROBANTES_VER + "')")
    @Operation(summary = "Descargar el CDR devuelto por SUNAT")
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
