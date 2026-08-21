package pe.com.proveperu.sgc.catalogo.api.controller;

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
import pe.com.proveperu.sgc.catalogo.api.dto.ProductoActualizarRequest;
import pe.com.proveperu.sgc.catalogo.api.dto.ProductoCrearRequest;
import pe.com.proveperu.sgc.catalogo.api.dto.ProductoEstadoRequest;
import pe.com.proveperu.sgc.catalogo.api.dto.ProductoResponse;
import pe.com.proveperu.sgc.catalogo.application.service.PermisosCatalogo;
import pe.com.proveperu.sgc.catalogo.application.service.ProductoService;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.config.OpenApiConfig;
import pe.com.proveperu.sgc.shared.api.dto.PaginaResponse;

@RestController
@RequestMapping("/api/v1/productos")
@RequiredArgsConstructor
@Validated
@Tag(name = "Productos", description = "Administración del catálogo de productos")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ProductoController {

    private final ProductoService productoService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisosCatalogo.PRODUCTOS_VER + "')")
    @Operation(summary = "Listar y buscar productos")
    public PaginaResponse<ProductoResponse> listar(
        @RequestParam(defaultValue = "")
        @Size(max = 180, message = "La búsqueda no puede superar 180 caracteres")
        String buscar,
        @RequestParam(required = false) EstadoCatalogo estado,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "nombre"));
        return productoService.listar(buscar, estado, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisosCatalogo.PRODUCTOS_VER + "')")
    @Operation(summary = "Consultar el detalle de un producto")
    public ProductoResponse obtener(@PathVariable Long id) {
        return productoService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermisosCatalogo.PRODUCTOS_CREAR + "')")
    @Operation(summary = "Crear un producto", description = "Puede registrar sus precios iniciales en la misma operación")
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoCrearRequest request) {
        ProductoResponse producto = productoService.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/productos/" + producto.id())).body(producto);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisosCatalogo.PRODUCTOS_EDITAR + "')")
    @Operation(summary = "Actualizar un producto")
    public ProductoResponse actualizar(
        @PathVariable Long id,
        @Valid @RequestBody ProductoActualizarRequest request
    ) {
        return productoService.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('" + PermisosCatalogo.PRODUCTOS_ESTADO + "')")
    @Operation(summary = "Activar o inactivar un producto")
    public ProductoResponse cambiarEstado(
        @PathVariable Long id,
        @Valid @RequestBody ProductoEstadoRequest request
    ) {
        return productoService.cambiarEstado(id, request.estado());
    }
}
