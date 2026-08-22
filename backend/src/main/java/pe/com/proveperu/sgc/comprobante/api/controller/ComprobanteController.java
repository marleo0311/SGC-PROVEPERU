package pe.com.proveperu.sgc.comprobante.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
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
import pe.com.proveperu.sgc.comprobante.api.dto.ComprobanteAnularRequest;
import pe.com.proveperu.sgc.comprobante.api.dto.ComprobanteResponse;
import pe.com.proveperu.sgc.comprobante.api.dto.RepresentacionComprobanteResponse;
import pe.com.proveperu.sgc.comprobante.application.service.ComprobanteService;
import pe.com.proveperu.sgc.config.OpenApiConfig;
import pe.com.proveperu.sgc.venta.api.dto.VentaAnularRequest;
import pe.com.proveperu.sgc.venta.application.service.PermisosVenta;
import pe.com.proveperu.sgc.venta.application.service.VentaService;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Tag(
    name = "Comprobantes",
    description = "Notas de venta, boletas y facturas asociadas a ventas"
)
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ComprobanteController {

    private final ComprobanteService comprobanteService;
    private final VentaService ventaService;

    @GetMapping("/comprobantes/{id}")
    @PreAuthorize("hasAuthority('" + PermisosVenta.COMPROBANTES_VER + "')")
    @Operation(summary = "Consultar un comprobante")
    public ComprobanteResponse obtener(@PathVariable @Positive Long id) {
        return comprobanteService.obtener(id);
    }

    @GetMapping("/ventas/{idVenta}/comprobante")
    @PreAuthorize("hasAuthority('" + PermisosVenta.COMPROBANTES_VER + "')")
    @Operation(summary = "Consultar el comprobante asociado a una venta")
    public ComprobanteResponse obtenerPorVenta(
        @PathVariable @Positive Long idVenta
    ) {
        return comprobanteService.obtenerPorVenta(idVenta);
    }

    @GetMapping("/comprobantes/{id}/representacion")
    @PreAuthorize("hasAuthority('" + PermisosVenta.COMPROBANTES_VER + "')")
    @Operation(
        summary = "Obtener la representación imprimible de un comprobante",
        description = "Devuelve datos estructurados para impresión; no acredita envío ni aceptación por SUNAT"
    )
    public RepresentacionComprobanteResponse obtenerRepresentacion(
        @PathVariable @Positive Long id
    ) {
        return comprobanteService.obtenerRepresentacion(id);
    }

    @PostMapping("/comprobantes/{id}/anular")
    @PreAuthorize("hasAuthority('" + PermisosVenta.COMPROBANTES_ANULAR + "')")
    @Operation(
        summary = "Anular un comprobante y su venta",
        description = "Revierte la venta mediante su flujo transaccional y conserva la trazabilidad del comprobante"
    )
    public ComprobanteResponse anular(
        @PathVariable @Positive Long id,
        @Valid @RequestBody ComprobanteAnularRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        Long idVenta = comprobanteService.obtenerIdVenta(id);
        ventaService.anular(
            idVenta,
            new VentaAnularRequest(request.motivo()),
            jwt.getSubject()
        );
        return comprobanteService.obtener(id);
    }
}
