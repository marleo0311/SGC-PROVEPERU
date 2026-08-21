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
import pe.com.proveperu.sgc.catalogo.api.dto.UnidadMedidaActualizarRequest;
import pe.com.proveperu.sgc.catalogo.api.dto.UnidadMedidaCrearRequest;
import pe.com.proveperu.sgc.catalogo.api.dto.UnidadMedidaResponse;
import pe.com.proveperu.sgc.catalogo.application.service.PermisosCatalogo;
import pe.com.proveperu.sgc.catalogo.application.service.UnidadMedidaService;
import pe.com.proveperu.sgc.catalogo.domain.model.EstadoCatalogo;
import pe.com.proveperu.sgc.config.OpenApiConfig;

@RestController
@RequestMapping("/api/v1/unidades-medida")
@RequiredArgsConstructor
@Validated
@Tag(name = "Unidades de medida", description = "Administración de unidades utilizadas por los productos")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class UnidadMedidaController {

    private final UnidadMedidaService unidadMedidaService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisosCatalogo.UNIDADES_VER + "')")
    @Operation(summary = "Listar y buscar unidades de medida")
    public List<UnidadMedidaResponse> listar(
        @RequestParam(defaultValue = "")
        @Size(max = 120, message = "La búsqueda no puede superar 120 caracteres")
        String buscar,
        @RequestParam(required = false) EstadoCatalogo estado
    ) {
        return unidadMedidaService.listar(buscar, estado);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermisosCatalogo.UNIDADES_CREAR + "')")
    @Operation(summary = "Crear una unidad de medida")
    public ResponseEntity<UnidadMedidaResponse> crear(
        @Valid @RequestBody UnidadMedidaCrearRequest request
    ) {
        UnidadMedidaResponse unidad = unidadMedidaService.crear(request);
        return ResponseEntity.created(URI.create("/api/v1/unidades-medida/" + unidad.id())).body(unidad);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisosCatalogo.UNIDADES_EDITAR + "')")
    @Operation(summary = "Actualizar una unidad de medida y su estado")
    public UnidadMedidaResponse actualizar(
        @PathVariable Long id,
        @Valid @RequestBody UnidadMedidaActualizarRequest request
    ) {
        return unidadMedidaService.actualizar(id, request);
    }
}
