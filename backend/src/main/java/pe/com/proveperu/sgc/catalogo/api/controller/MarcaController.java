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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.proveperu.sgc.catalogo.api.dto.MarcaActualizarRequest;
import pe.com.proveperu.sgc.catalogo.api.dto.MarcaCrearRequest;
import pe.com.proveperu.sgc.catalogo.api.dto.MarcaResponse;
import pe.com.proveperu.sgc.catalogo.application.service.MarcaService;
import pe.com.proveperu.sgc.catalogo.application.service.PermisosCatalogo;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.config.OpenApiConfig;

@RestController
@RequestMapping("/api/v1/marcas")
@RequiredArgsConstructor
@Validated
@Tag(name = "Marcas", description = "Administración de marcas de productos")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class MarcaController {

    private final MarcaService marcaService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisosCatalogo.MARCAS_VER + "')")
    @Operation(summary = "Listar y buscar marcas")
    public List<MarcaResponse> listar(
        @RequestParam(defaultValue = "")
        @Size(max = 120, message = "La búsqueda no puede superar 120 caracteres")
        String buscar,
        @RequestParam(required = false) EstadoCatalogo estado
    ) {
        return marcaService.listar(buscar, estado);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermisosCatalogo.MARCAS_CREAR + "')")
    @Operation(summary = "Crear una marca")
    public ResponseEntity<MarcaResponse> crear(@Valid @RequestBody MarcaCrearRequest request) {
        MarcaResponse marca = marcaService.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/marcas/" + marca.id())).body(marca);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisosCatalogo.MARCAS_EDITAR + "')")
    @Operation(summary = "Actualizar una marca y su estado")
    public MarcaResponse actualizar(
        @PathVariable Long id,
        @Valid @RequestBody MarcaActualizarRequest request
    ) {
        return marcaService.actualizar(id, request);
    }
}
