package pe.com.proveperu.sgc.impresion.api.controller;

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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.proveperu.sgc.config.OpenApiConfig;
import pe.com.proveperu.sgc.impresion.api.dto.TicketResponse;
import pe.com.proveperu.sgc.impresion.application.service.ComprobantePdfService;
import pe.com.proveperu.sgc.impresion.application.service.PermisosImpresion;
import pe.com.proveperu.sgc.impresion.application.service.TicketService;
import pe.com.proveperu.sgc.impresion.domain.model.FormatoTicket;

@RestController
@RequestMapping("/api/v1/impresiones")
@RequiredArgsConstructor
@Validated
@Tag(
    name = "Impresiones",
    description = "Formatos térmicos y representaciones PDF de comprobantes"
)
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class TicketController {

    private final TicketService ticketService;
    private final ComprobantePdfService comprobantePdfService;

    @GetMapping("/ticket/{idComprobante}")
    @PreAuthorize("hasAuthority('" + PermisosImpresion.TICKETS_IMPRIMIR + "')")
    @Operation(
        summary = "Generar ticket térmico de un comprobante",
        description = "Devuelve texto UTF-8 para papel de 58 mm u 80 mm; no incluye comandos ESC/POS ni envía información directamente a una impresora"
    )
    public TicketResponse generar(
        @PathVariable @Positive Long idComprobante,
        @RequestParam(defaultValue = "MM80") FormatoTicket formato
    ) {
        return ticketService.generar(idComprobante, formato);
    }

    @GetMapping("/comprobante/{idComprobante}/pdf")
    @PreAuthorize("hasAuthority('" + PermisosImpresion.TICKETS_IMPRIMIR + "')")
    @Operation(
        summary = "Descargar la representación PDF de un comprobante",
        description = "Genera un PDF A4 con emisor, cliente, detalle, totales, estado y código QR; no sustituye el XML ni el CDR"
    )
    public ResponseEntity<byte[]> descargarPdf(
        @PathVariable @Positive Long idComprobante
    ) {
        var archivo = comprobantePdfService.generar(idComprobante);
        ContentDisposition disposition = ContentDisposition.attachment()
            .filename(archivo.nombre(), StandardCharsets.UTF_8)
            .build();
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .contentLength(archivo.contenido().length)
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .body(archivo.contenido());
    }
}
