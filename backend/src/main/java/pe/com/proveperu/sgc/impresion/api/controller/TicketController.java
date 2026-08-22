package pe.com.proveperu.sgc.impresion.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.proveperu.sgc.config.OpenApiConfig;
import pe.com.proveperu.sgc.impresion.api.dto.TicketResponse;
import pe.com.proveperu.sgc.impresion.application.service.PermisosImpresion;
import pe.com.proveperu.sgc.impresion.application.service.TicketService;
import pe.com.proveperu.sgc.impresion.domain.model.FormatoTicket;

@RestController
@RequestMapping("/api/v1/impresiones")
@RequiredArgsConstructor
@Validated
@Tag(
    name = "Impresiones",
    description = "Formatos térmicos para impresión local de comprobantes"
)
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class TicketController {

    private final TicketService ticketService;

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
}
