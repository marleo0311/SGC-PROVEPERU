package pe.com.proveperu.sgc.inventario.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.LocalDate;
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
import pe.com.proveperu.sgc.inventario.api.dto.AjusteInventarioRequest;
import pe.com.proveperu.sgc.inventario.api.dto.AjusteInventarioResponse;
import pe.com.proveperu.sgc.inventario.api.dto.MovimientoInventarioResponse;
import pe.com.proveperu.sgc.inventario.api.dto.ExistenciaPresentacionResponse;
import pe.com.proveperu.sgc.inventario.api.dto.IngresoPresentacionesRequest;
import pe.com.proveperu.sgc.inventario.api.dto.IngresoPresentacionesResponse;
import pe.com.proveperu.sgc.inventario.api.dto.StockInventarioResponse;
import pe.com.proveperu.sgc.inventario.api.dto.StockMinimoInventarioRequest;
import pe.com.proveperu.sgc.inventario.api.dto.TransferenciaInventarioRequest;
import pe.com.proveperu.sgc.inventario.api.dto.TransferenciaInventarioResponse;
import pe.com.proveperu.sgc.inventario.application.service.InventarioService;
import pe.com.proveperu.sgc.inventario.application.service.PermisosInventario;
import pe.com.proveperu.sgc.inventario.domain.model.TipoMovimientoInventario;
import pe.com.proveperu.sgc.inventario.domain.model.EstadoExistenciaPresentacion;
import java.util.List;
import pe.com.proveperu.sgc.shared.api.dto.PaginaResponse;

@RestController
@RequestMapping("/api/v1/inventario")
@RequiredArgsConstructor
@Validated
@Tag(name = "Inventario", description = "Stock, ajustes y movimientos de inventario")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class InventarioController {

    private final InventarioService inventarioService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisosInventario.STOCK_VER + "')")
    @Operation(summary = "Consultar stock por sede y producto")
    public PaginaResponse<StockInventarioResponse> listar(
        @RequestParam(required = false) @Positive Long idSede,
        @RequestParam(defaultValue = "") @Size(max = 180) String buscar,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("nombre").ascending());
        return inventarioService.listar(idSede, buscar, pageable);
    }

    @GetMapping("/stock-bajo")
    @PreAuthorize("hasAuthority('" + PermisosInventario.STOCK_VER + "')")
    @Operation(summary = "Listar productos agotados o con stock bajo")
    public PaginaResponse<StockInventarioResponse> listarStockBajo(
        @RequestParam(required = false) @Positive Long idSede,
        @RequestParam(defaultValue = "") @Size(max = 180) String buscar,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("nombre").ascending());
        return inventarioService.listarStockBajo(idSede, buscar, pageable);
    }

    @GetMapping("/movimientos")
    @PreAuthorize("hasAuthority('" + PermisosInventario.MOVIMIENTOS_VER + "')")
    @Operation(summary = "Consultar movimientos de inventario con filtros")
    public PaginaResponse<MovimientoInventarioResponse> listarMovimientos(
        @RequestParam(required = false) @Positive Long idSede,
        @RequestParam(required = false) @Positive Long idProducto,
        @RequestParam(required = false) TipoMovimientoInventario tipo,
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
            Sort.by(Sort.Order.desc("fechaHora"), Sort.Order.desc("id"))
        );
        return inventarioService.listarMovimientos(
            idSede,
            idProducto,
            tipo,
            desde,
            hasta,
            pageable
        );
    }

    @GetMapping("/{idProducto}")
    @PreAuthorize("hasAuthority('" + PermisosInventario.STOCK_VER + "')")
    @Operation(summary = "Consultar stock físico, reservado y disponible de un producto")
    public StockInventarioResponse obtener(
        @PathVariable @Positive Long idProducto,
        @RequestParam(required = false) @Positive Long idSede
    ) {
        return inventarioService.obtener(idProducto, idSede);
    }

    @GetMapping("/{idProducto}/presentaciones")
    @PreAuthorize("hasAnyAuthority('" + PermisosInventario.STOCK_VER + "', '"
        + PermisosInventario.PRESENTACIONES_GESTIONAR + "')")
    @Operation(summary = "Listar cajas, paquetes y rollos físicos de un producto")
    public List<ExistenciaPresentacionResponse> listarPresentaciones(
        @PathVariable @Positive Long idProducto,
        @RequestParam(required = false) @Positive Long idSede,
        @RequestParam(required = false) EstadoExistenciaPresentacion estado
    ) {
        return inventarioService.listarPresentaciones(idSede, idProducto, estado);
    }

    @PostMapping("/presentaciones")
    @PreAuthorize("hasAuthority('" + PermisosInventario.PRESENTACIONES_GESTIONAR + "')")
    @Operation(summary = "Registrar el contenido real de cajas, paquetes o rollos")
    public ResponseEntity<IngresoPresentacionesResponse> registrarPresentaciones(
        @Valid @RequestBody IngresoPresentacionesRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        IngresoPresentacionesResponse response = inventarioService
            .registrarPresentaciones(request, jwt.getSubject());
        return ResponseEntity.created(URI.create(
            "/api/v1/inventario/" + request.idProducto() + "/presentaciones"
        )).body(response);
    }

    @PatchMapping("/presentaciones/{id}/abrir")
    @PreAuthorize("hasAuthority('" + PermisosInventario.PRESENTACIONES_GESTIONAR + "')")
    @Operation(summary = "Abrir una presentación para vender por unidad o metro")
    public ExistenciaPresentacionResponse abrirPresentacion(
        @PathVariable @Positive Long id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return inventarioService.abrirPresentacion(id, jwt.getSubject());
    }

    @PostMapping("/ajustes")
    @PreAuthorize("hasAuthority('" + PermisosInventario.AJUSTES_CREAR + "')")
    @Operation(
        summary = "Registrar un ajuste positivo o negativo",
        description = "Convierte la cantidad a la unidad base y registra el usuario autenticado en Kardex"
    )
    public ResponseEntity<AjusteInventarioResponse> ajustar(
        @Valid @RequestBody AjusteInventarioRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        AjusteInventarioResponse response = inventarioService.ajustar(request, jwt.getSubject());
        URI location = URI.create(
            "/api/v1/inventario/movimientos/" + response.movimiento().id()
        );
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/transferencias")
    @PreAuthorize("hasAuthority('" + PermisosInventario.TRANSFERENCIAS_CREAR + "')")
    @Operation(
        summary = "Transferir existencias entre almacenes",
        description = "Registra una salida y una entrada enlazadas dentro de una sola transacción"
    )
    public ResponseEntity<TransferenciaInventarioResponse> transferir(
        @Valid @RequestBody TransferenciaInventarioRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        TransferenciaInventarioResponse response = inventarioService.transferir(
            request,
            jwt.getSubject()
        );
        return ResponseEntity
            .created(URI.create("/api/v1/inventario/transferencias/" + response.id()))
            .body(response);
    }

    @PutMapping("/{idProducto}/stock-minimo")
    @PreAuthorize("hasAuthority('" + PermisosInventario.MINIMOS_EDITAR + "')")
    @Operation(summary = "Configurar el stock mínimo de un producto por almacén")
    public StockInventarioResponse actualizarStockMinimo(
        @PathVariable @Positive Long idProducto,
        @Valid @RequestBody StockMinimoInventarioRequest request
    ) {
        return inventarioService.actualizarStockMinimo(idProducto, request);
    }
}
