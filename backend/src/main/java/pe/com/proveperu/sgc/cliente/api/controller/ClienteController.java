package pe.com.proveperu.sgc.cliente.api.controller;

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
import pe.com.proveperu.sgc.cliente.api.dto.ClienteEstadoRequest;
import pe.com.proveperu.sgc.cliente.api.dto.ClienteGuardarRequest;
import pe.com.proveperu.sgc.cliente.api.dto.ClienteHistorialResponse;
import pe.com.proveperu.sgc.cliente.api.dto.ClienteResponse;
import pe.com.proveperu.sgc.cliente.application.service.ClienteCreacionResultado;
import pe.com.proveperu.sgc.cliente.application.service.ClienteService;
import pe.com.proveperu.sgc.cliente.application.service.PermisosCliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoPersona;
import pe.com.proveperu.sgc.config.OpenApiConfig;
import pe.com.proveperu.sgc.shared.api.dto.PaginaResponse;

@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
@Validated
@Tag(name = "Clientes", description = "Gestión de personas naturales y jurídicas")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisosCliente.CLIENTES_VER + "')")
    @Operation(summary = "Listar y buscar clientes")
    public PaginaResponse<ClienteResponse> listar(
        @RequestParam(defaultValue = "")
        @Size(max = 200, message = "La búsqueda no puede superar 200 caracteres")
        String buscar,
        @RequestParam(required = false) EstadoCatalogo estado,
        @RequestParam(required = false) TipoPersona tipoPersona,
        @RequestParam(required = false) Boolean permiteCredito,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        PageRequest pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Direction.ASC, "numeroDocumento")
        );
        return clienteService.listar(buscar, estado, tipoPersona, permiteCredito, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisosCliente.CLIENTES_VER + "')")
    @Operation(summary = "Consultar un cliente")
    public ClienteResponse obtener(@PathVariable Long id) {
        return clienteService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermisosCliente.CLIENTES_CREAR + "')")
    @Operation(
        summary = "Registrar un cliente",
        description = "Si el documento ya existe, devuelve el cliente registrado sin duplicarlo"
    )
    public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody ClienteGuardarRequest request) {
        ClienteCreacionResultado resultado = clienteService.crear(request);
        if (!resultado.creado()) {
            return ResponseEntity.ok()
                .header("X-Recurso-Existente", "true")
                .body(resultado.cliente());
        }
        return ResponseEntity
            .created(URI.create("/api/v1/clientes/" + resultado.cliente().id()))
            .body(resultado.cliente());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisosCliente.CLIENTES_EDITAR + "')")
    @Operation(summary = "Actualizar un cliente")
    public ClienteResponse actualizar(
        @PathVariable Long id,
        @Valid @RequestBody ClienteGuardarRequest request
    ) {
        return clienteService.actualizar(id, request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('" + PermisosCliente.CLIENTES_ESTADO + "')")
    @Operation(summary = "Activar o inactivar un cliente")
    public ClienteResponse cambiarEstado(
        @PathVariable Long id,
        @Valid @RequestBody ClienteEstadoRequest request
    ) {
        return clienteService.cambiarEstado(id, request.estado());
    }

    @GetMapping("/{id}/historial")
    @PreAuthorize("hasAuthority('" + PermisosCliente.HISTORIAL_VER + "')")
    @Operation(
        summary = "Consultar el historial del cliente",
        description = "Incluye el resumen comercial, las operaciones disponibles y los precios especiales"
    )
    public ClienteHistorialResponse obtenerHistorial(@PathVariable Long id) {
        return clienteService.obtenerHistorial(id);
    }
}
