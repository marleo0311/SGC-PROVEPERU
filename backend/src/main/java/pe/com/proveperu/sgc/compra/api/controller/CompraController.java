package pe.com.proveperu.sgc.compra.api.controller;

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
import pe.com.proveperu.sgc.compra.api.dto.CompraEstadoRequest;
import pe.com.proveperu.sgc.compra.api.dto.CompraGuardarRequest;
import pe.com.proveperu.sgc.compra.api.dto.CompraResponse;
import pe.com.proveperu.sgc.compra.api.dto.CompraResumenResponse;
import pe.com.proveperu.sgc.compra.application.service.CompraService;
import pe.com.proveperu.sgc.compra.application.service.PermisosCompra;
import pe.com.proveperu.sgc.compra.domain.model.EstadoCompra;
import pe.com.proveperu.sgc.config.OpenApiConfig;
import pe.com.proveperu.sgc.shared.api.dto.PaginaResponse;
import pe.com.proveperu.sgc.transporte.api.dto.GastoCrearRequest;
import pe.com.proveperu.sgc.transporte.api.dto.GastoResponse;
import pe.com.proveperu.sgc.transporte.application.service.PermisosTransporte;

@RestController
@RequestMapping("/api/v1/compras")
@RequiredArgsConstructor
@Validated
@Tag(
    name = "Compras",
    description = "Registro de compras, productos adquiridos y gastos relacionados"
)
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class CompraController {

    private final CompraService compraService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisosCompra.COMPRAS_VER + "')")
    @Operation(summary = "Listar compras y filtrar por proveedor, fecha o estado")
    public PaginaResponse<CompraResumenResponse> listar(
        @RequestParam(required = false) @Positive Long idProveedor,
        @RequestParam(required = false) EstadoCompra estado,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate desde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate hasta,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        PageRequest pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id"))
        );
        return compraService.listar(idProveedor, estado, desde, hasta, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisosCompra.COMPRAS_VER + "')")
    @Operation(summary = "Consultar una compra con todos sus detalles")
    public CompraResponse obtener(@PathVariable @Positive Long id) {
        return compraService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermisosCompra.COMPRAS_CREAR + "')")
    @Operation(
        summary = "Registrar una compra",
        description = "Calcula subtotales y total, identifica al usuario autenticado y no modifica el inventario"
    )
    public ResponseEntity<CompraResponse> crear(
        @Valid @RequestBody CompraGuardarRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        CompraResponse compra = compraService.crear(request, jwt.getSubject());
        return ResponseEntity.created(URI.create("/api/v1/compras/" + compra.id()))
            .body(compra);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisosCompra.COMPRAS_EDITAR + "')")
    @Operation(summary = "Editar una compra mientras se encuentre REGISTRADA")
    public CompraResponse actualizar(
        @PathVariable @Positive Long id,
        @Valid @RequestBody CompraGuardarRequest request
    ) {
        return compraService.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('" + PermisosCompra.COMPRAS_ANULAR + "')")
    @Operation(
        summary = "Anular una compra",
        description = "V10 solo permite cambiar a ANULADA; los estados de recepción se gestionarán al confirmar mercadería"
    )
    public CompraResponse cambiarEstado(
        @PathVariable @Positive Long id,
        @Valid @RequestBody CompraEstadoRequest request
    ) {
        return compraService.cambiarEstado(id, request.estado());
    }

    @PostMapping("/{id}/gastos")
    @PreAuthorize("hasAuthority('" + PermisosTransporte.GASTOS_CREAR + "')")
    @Operation(
        summary = "Registrar un gasto relacionado con la compra",
        description = "Recalcula automáticamente los gastos adicionales y el total de la compra"
    )
    public ResponseEntity<GastoResponse> crearGasto(
        @PathVariable @Positive Long id,
        @Valid @RequestBody GastoCrearRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        GastoResponse gasto = compraService.crearGasto(id, request, jwt.getSubject());
        return ResponseEntity.created(URI.create("/api/v1/gastos/" + gasto.id()))
            .body(gasto);
    }

    @GetMapping("/{id}/gastos")
    @PreAuthorize("hasAuthority('" + PermisosTransporte.GASTOS_VER + "')")
    @Operation(summary = "Consultar los gastos relacionados con una compra")
    public List<GastoResponse> listarGastos(@PathVariable @Positive Long id) {
        return compraService.listarGastos(id);
    }
}
