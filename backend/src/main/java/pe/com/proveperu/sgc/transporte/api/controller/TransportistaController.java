package pe.com.proveperu.sgc.transporte.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
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
import pe.com.proveperu.sgc.shared.api.dto.PaginaResponse;
import pe.com.proveperu.sgc.transporte.api.dto.GastoResponse;
import pe.com.proveperu.sgc.transporte.api.dto.TransportistaEstadoRequest;
import pe.com.proveperu.sgc.transporte.api.dto.TransportistaGuardarRequest;
import pe.com.proveperu.sgc.transporte.api.dto.TransportistaResponse;
import pe.com.proveperu.sgc.transporte.application.service.PermisosTransporte;
import pe.com.proveperu.sgc.transporte.application.service.TransportistaService;

@RestController
@RequestMapping("/api/v1/transportistas")
@RequiredArgsConstructor
@Validated
@Tag(name = "Transportistas", description = "Transportistas y sus gastos relacionados")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class TransportistaController {

    private final TransportistaService transportistaService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + PermisosTransporte.TRANSPORTISTAS_VER + "', '"
        + PermisosTransporte.GASTOS_CREAR + "')")
    @Operation(summary = "Listar y buscar transportistas")
    public PaginaResponse<TransportistaResponse> listar(
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
            Sort.by(Sort.Direction.ASC, "nombreRazonSocial")
        );
        return transportistaService.listar(buscar, estado, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisosTransporte.TRANSPORTISTAS_VER + "')")
    @Operation(summary = "Consultar un transportista")
    public TransportistaResponse obtener(@PathVariable Long id) {
        return transportistaService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermisosTransporte.TRANSPORTISTAS_CREAR + "')")
    @Operation(summary = "Registrar un transportista")
    public ResponseEntity<TransportistaResponse> crear(
        @Valid @RequestBody TransportistaGuardarRequest request
    ) {
        TransportistaResponse transportista = transportistaService.crear(request);
        return ResponseEntity.created(URI.create(
            "/api/v1/transportistas/" + transportista.id()
        )).body(transportista);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisosTransporte.TRANSPORTISTAS_EDITAR + "')")
    @Operation(summary = "Actualizar un transportista")
    public TransportistaResponse actualizar(
        @PathVariable Long id,
        @Valid @RequestBody TransportistaGuardarRequest request
    ) {
        return transportistaService.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('" + PermisosTransporte.TRANSPORTISTAS_ESTADO + "')")
    @Operation(summary = "Activar o inactivar un transportista")
    public TransportistaResponse cambiarEstado(
        @PathVariable Long id,
        @Valid @RequestBody TransportistaEstadoRequest request
    ) {
        return transportistaService.cambiarEstado(id, request.estado());
    }

    @GetMapping("/{id}/gastos")
    @PreAuthorize("hasAuthority('" + PermisosTransporte.GASTOS_VER + "')")
    @Operation(summary = "Consultar los gastos relacionados con un transportista")
    public List<GastoResponse> listarGastos(@PathVariable Long id) {
        return transportistaService.listarGastos(id);
    }
}
