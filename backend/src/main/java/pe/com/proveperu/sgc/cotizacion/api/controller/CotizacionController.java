package pe.com.proveperu.sgc.cotizacion.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.proveperu.sgc.config.OpenApiConfig;
import pe.com.proveperu.sgc.cotizacion.api.dto.CotizacionEstadoRequest;
import pe.com.proveperu.sgc.cotizacion.api.dto.CotizacionGuardarRequest;
import pe.com.proveperu.sgc.cotizacion.api.dto.CotizacionResponse;
import pe.com.proveperu.sgc.cotizacion.api.dto.CotizacionResumenResponse;
import pe.com.proveperu.sgc.cotizacion.application.service.CotizacionService;
import pe.com.proveperu.sgc.cotizacion.application.service.PermisosCotizacion;
import pe.com.proveperu.sgc.cotizacion.domain.model.EstadoCotizacion;
import pe.com.proveperu.sgc.shared.api.dto.PaginaResponse;

@RestController
@RequestMapping("/api/v1/cotizaciones")
@RequiredArgsConstructor
@Validated
@Tag(
    name = "Cotizaciones",
    description = "Propuestas comerciales con precios, descuentos, vigencia y disponibilidad"
)
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class CotizacionController {

    private final CotizacionService cotizacionService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisosCotizacion.COTIZACIONES_VER + "')")
    @Operation(summary = "Listar cotizaciones y filtrar por cliente, fecha o estado")
    public PaginaResponse<CotizacionResumenResponse> listar(
        @RequestParam(required = false) @Positive Long idCliente,
        @RequestParam(required = false) EstadoCotizacion estado,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate desde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate hasta,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return cotizacionService.listar(
            idCliente,
            estado,
            desde,
            hasta,
            PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id"))
            )
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisosCotizacion.COTIZACIONES_VER + "')")
    @Operation(summary = "Consultar una cotización y la disponibilidad actual")
    public CotizacionResponse obtener(@PathVariable @Positive Long id) {
        return cotizacionService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermisosCotizacion.COTIZACIONES_CREAR + "')")
    @Operation(
        summary = "Crear una cotización",
        description = "Resuelve precios vigentes, aplica descuentos autorizados y no reserva inventario"
    )
    public ResponseEntity<CotizacionResponse> crear(
        @Valid @RequestBody CotizacionGuardarRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        CotizacionResponse cotizacion = cotizacionService.crear(
            request,
            jwt.getSubject(),
            tienePermisoDescuento(jwt)
        );
        Long id = cotizacion.cotizacion().id();
        return ResponseEntity.created(URI.create("/api/v1/cotizaciones/" + id))
            .body(cotizacion);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisosCotizacion.COTIZACIONES_EDITAR + "')")
    @Operation(summary = "Editar una cotización PENDIENTE y vigente")
    public CotizacionResponse actualizar(
        @PathVariable @Positive Long id,
        @Valid @RequestBody CotizacionGuardarRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return cotizacionService.actualizar(
            id,
            request,
            tienePermisoDescuento(jwt)
        );
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('" + PermisosCotizacion.COTIZACIONES_ESTADO + "')")
    @Operation(
        summary = "Aceptar o rechazar una cotización",
        description = "Las conversiones a pedido y venta se habilitarán con sus respectivos módulos"
    )
    public CotizacionResponse cambiarEstado(
        @PathVariable @Positive Long id,
        @Valid @RequestBody CotizacionEstadoRequest request
    ) {
        return cotizacionService.cambiarEstado(id, request.estado());
    }

    private boolean tienePermisoDescuento(Jwt jwt) {
        List<String> authorities = jwt.getClaimAsStringList("authorities");
        return authorities != null
            && authorities.contains(PermisosCotizacion.DESCUENTOS_APLICAR);
    }
}
