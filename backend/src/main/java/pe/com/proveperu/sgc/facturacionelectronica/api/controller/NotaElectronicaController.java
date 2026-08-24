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
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.NotaElectronicaCrearRequest;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.NotaElectronicaResponse;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.ArchivoElectronico;
import pe.com.proveperu.sgc.facturacionelectronica.application.service.NotaElectronicaService;
import pe.com.proveperu.sgc.venta.application.service.PermisosVenta;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Tag(name = "Notas electrónicas SUNAT", description = "Notas de crédito y débito vinculadas a comprobantes aceptados")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class NotaElectronicaController {

    private final NotaElectronicaService service;

    @GetMapping("/comprobantes/{id}/notas-electronicas")
    @PreAuthorize("hasAuthority('" + PermisosVenta.COMPROBANTES_VER + "')")
    @Operation(summary = "Listar las notas electrónicas de un comprobante")
    public List<NotaElectronicaResponse> listar(@PathVariable @Positive Long id) {
        return service.listar(id);
    }

    @PostMapping("/comprobantes/{id}/notas-electronicas")
    @PreAuthorize("hasAuthority('" + PermisosVenta.SUNAT_NOTAS_GESTIONAR + "')")
    @Operation(summary = "Generar y firmar una nota de crédito o débito")
    public NotaElectronicaResponse crear(
        @PathVariable @Positive Long id,
        @Valid @RequestBody NotaElectronicaCrearRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.crear(id, request, jwt.getSubject());
    }

    @GetMapping("/notas-electronicas/{id}")
    @PreAuthorize("hasAuthority('" + PermisosVenta.COMPROBANTES_VER + "')")
    public NotaElectronicaResponse detalle(@PathVariable @Positive Long id) {
        return service.detalle(id);
    }

    @PostMapping("/notas-electronicas/{id}/enviar")
    @PreAuthorize("hasAuthority('" + PermisosVenta.SUNAT_NOTAS_GESTIONAR + "')")
    public NotaElectronicaResponse enviar(@PathVariable @Positive Long id) {
        return service.enviar(id);
    }

    @GetMapping("/notas-electronicas/{id}/xml")
    @PreAuthorize("hasAuthority('" + PermisosVenta.COMPROBANTES_VER + "')")
    public ResponseEntity<byte[]> xml(@PathVariable @Positive Long id) {
        return archivo(service.xml(id));
    }

    @GetMapping("/notas-electronicas/{id}/cdr")
    @PreAuthorize("hasAuthority('" + PermisosVenta.COMPROBANTES_VER + "')")
    public ResponseEntity<byte[]> cdr(@PathVariable @Positive Long id) {
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
