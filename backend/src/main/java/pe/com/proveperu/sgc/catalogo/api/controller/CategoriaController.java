package pe.com.proveperu.sgc.catalogo.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
import pe.com.proveperu.sgc.catalogo.api.dto.CategoriaActualizarRequest;
import pe.com.proveperu.sgc.catalogo.api.dto.CategoriaCrearRequest;
import pe.com.proveperu.sgc.catalogo.api.dto.CategoriaEstadoRequest;
import pe.com.proveperu.sgc.catalogo.api.dto.CategoriaResponse;
import pe.com.proveperu.sgc.catalogo.application.service.CategoriaService;
import pe.com.proveperu.sgc.catalogo.application.service.PermisosCatalogo;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.config.OpenApiConfig;

@RestController
@RequestMapping("/api/v1/categorias")
@RequiredArgsConstructor
@Validated
@Tag(name = "Categorías", description = "Administración de categorías de productos")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class CategoriaController {

    private final CategoriaService categoriaService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisosCatalogo.CATEGORIAS_VER + "')")
    @Operation(summary = "Listar y buscar categorías")
    public List<CategoriaResponse> listar(
        @RequestParam(defaultValue = "")
        @Size(max = 120, message = "La búsqueda no puede superar 120 caracteres")
        String buscar,
        @RequestParam(required = false) EstadoCatalogo estado
    ) {
        return categoriaService.listar(buscar, estado);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermisosCatalogo.CATEGORIAS_CREAR + "')")
    @Operation(summary = "Crear una categoría")
    public ResponseEntity<CategoriaResponse> crear(@Valid @RequestBody CategoriaCrearRequest request) {
        CategoriaResponse categoria = categoriaService.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/categorias/" + categoria.id())).body(categoria);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisosCatalogo.CATEGORIAS_EDITAR + "')")
    @Operation(summary = "Actualizar una categoría")
    public CategoriaResponse actualizar(
        @PathVariable Long id,
        @Valid @RequestBody CategoriaActualizarRequest request
    ) {
        return categoriaService.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('" + PermisosCatalogo.CATEGORIAS_ESTADO + "')")
    @Operation(summary = "Activar o inactivar una categoría")
    public CategoriaResponse cambiarEstado(
        @PathVariable Long id,
        @Valid @RequestBody CategoriaEstadoRequest request
    ) {
        return categoriaService.cambiarEstado(id, request.estado());
    }
}
