package pe.com.proveperu.sgc.facturacionelectronica.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.proveperu.sgc.config.OpenApiConfig;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.BajaComprobanteRequest;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.ComunicacionBajaResponse;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.ArchivoElectronico;
import pe.com.proveperu.sgc.facturacionelectronica.application.service.ComunicacionBajaService;
import pe.com.proveperu.sgc.venta.application.service.PermisosVenta;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Tag(name = "Bajas electrónicas SUNAT", description = "Comunicación de baja de facturas y anulación de boletas mediante resumen diario")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ComunicacionBajaController {
    private final ComunicacionBajaService service;

    @PostMapping("/comprobantes/{id}/sunat/baja")
    @PreAuthorize("hasAuthority('" + PermisosVenta.SUNAT_BAJAS_GESTIONAR + "')")
    @Operation(summary = "Solicitar la baja electrónica de un comprobante aceptado")
    public ComunicacionBajaResponse solicitar(@PathVariable @Positive Long id, @Valid @RequestBody BajaComprobanteRequest request, @AuthenticationPrincipal Jwt jwt) {
        return service.solicitar(id, request.motivo(), jwt.getSubject());
    }

    @GetMapping("/sunat/comunicaciones-baja")
    @PreAuthorize("hasAuthority('" + PermisosVenta.COMPROBANTES_VER + "')")
    public List<ComunicacionBajaResponse> listar() { return service.listar(); }
    @GetMapping("/sunat/comunicaciones-baja/{id}")
    @PreAuthorize("hasAuthority('" + PermisosVenta.COMPROBANTES_VER + "')")
    public ComunicacionBajaResponse detalle(@PathVariable @Positive Long id) { return service.detalle(id); }
    @PostMapping("/sunat/comunicaciones-baja/{id}/enviar")
    @PreAuthorize("hasAuthority('" + PermisosVenta.SUNAT_BAJAS_GESTIONAR + "')")
    public ComunicacionBajaResponse enviar(@PathVariable @Positive Long id) { return service.enviar(id); }
    @PostMapping("/sunat/comunicaciones-baja/{id}/consultar")
    @PreAuthorize("hasAuthority('" + PermisosVenta.SUNAT_BAJAS_GESTIONAR + "')")
    public ComunicacionBajaResponse consultar(@PathVariable @Positive Long id) { return service.consultar(id); }
    @GetMapping("/sunat/comunicaciones-baja/{id}/xml")
    @PreAuthorize("hasAuthority('" + PermisosVenta.COMPROBANTES_VER + "')")
    public ResponseEntity<byte[]> xml(@PathVariable @Positive Long id) { return archivo(service.xml(id)); }
    @GetMapping("/sunat/comunicaciones-baja/{id}/cdr")
    @PreAuthorize("hasAuthority('" + PermisosVenta.COMPROBANTES_VER + "')")
    public ResponseEntity<byte[]> cdr(@PathVariable @Positive Long id) { return archivo(service.cdr(id)); }

    private ResponseEntity<byte[]> archivo(ArchivoElectronico archivo) {
        ContentDisposition disposition = ContentDisposition.attachment().filename(archivo.nombre(), StandardCharsets.UTF_8).build();
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString()).contentType(MediaType.parseMediaType(archivo.contentType())).contentLength(archivo.contenido().length).body(archivo.contenido());
    }
}
