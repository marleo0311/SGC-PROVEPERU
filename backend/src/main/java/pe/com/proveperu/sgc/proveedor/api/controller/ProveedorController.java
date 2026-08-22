package pe.com.proveperu.sgc.proveedor.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.config.OpenApiConfig;
import pe.com.proveperu.sgc.proveedor.api.dto.ProveedorEstadoRequest;
import pe.com.proveperu.sgc.proveedor.api.dto.ProveedorGuardarRequest;
import pe.com.proveperu.sgc.proveedor.api.dto.ProveedorHistorialResponse;
import pe.com.proveperu.sgc.proveedor.api.dto.ProveedorResponse;
import pe.com.proveperu.sgc.proveedor.application.service.PermisosProveedor;
import pe.com.proveperu.sgc.proveedor.application.service.ProveedorService;
import pe.com.proveperu.sgc.shared.api.dto.PaginaResponse;

@RestController
@RequestMapping("/api/v1/proveedores")
@RequiredArgsConstructor
@Validated
@Tag(name = "Proveedores", description = "Gestión de proveedores e historial de compras")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ProveedorController {

    private final ProveedorService proveedorService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisosProveedor.PROVEEDORES_VER + "')")
    @Operation(summary = "Listar y buscar proveedores")
    public PaginaResponse<ProveedorResponse> listar(
        @RequestParam(defaultValue = "")
        @Size(max = 200, message = "La búsqueda no puede superar 200 caracteres")
        String buscar,
        @RequestParam(required = false) EstadoCatalogo estado,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        PageRequest pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.ASC, "razonSocial")
        );
        return proveedorService.listar(buscar, estado, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisosProveedor.PROVEEDORES_VER + "')")
    @Operation(summary = "Consultar un proveedor")
    public ProveedorResponse obtener(@PathVariable Long id) {
        return proveedorService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermisosProveedor.PROVEEDORES_CREAR + "')")
    @Operation(summary = "Registrar un proveedor")
    public ResponseEntity<ProveedorResponse> crear(
        @Valid @RequestBody ProveedorGuardarRequest request
    ) {
        ProveedorResponse proveedor = proveedorService.crear(request);
        return ResponseEntity.created(URI.create(
            "/api/v1/proveedores/" + proveedor.id()
        )).body(proveedor);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisosProveedor.PROVEEDORES_EDITAR + "')")
    @Operation(summary = "Actualizar un proveedor")
    public ProveedorResponse actualizar(
        @PathVariable Long id,
        @Valid @RequestBody ProveedorGuardarRequest request
    ) {
        return proveedorService.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('" + PermisosProveedor.PROVEEDORES_ESTADO + "')")
    @Operation(summary = "Activar o inactivar un proveedor")
    public ProveedorResponse cambiarEstado(
        @PathVariable Long id,
        @Valid @RequestBody ProveedorEstadoRequest request
    ) {
        return proveedorService.cambiarEstado(id, request.estado());
    }

    @GetMapping("/{id}/compras")
    @PreAuthorize("hasAuthority('" + PermisosProveedor.HISTORIAL_VER + "')")
    @Operation(
        summary = "Consultar el historial de compras de un proveedor",
        description = "Incluye documentos, fechas, importes y estados disponibles"
    )
    public ProveedorHistorialResponse obtenerHistorialCompras(@PathVariable Long id) {
        return proveedorService.obtenerHistorialCompras(id);
    }
}
