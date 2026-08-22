package pe.com.proveperu.sgc.cliente.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.proveperu.sgc.cliente.api.dto.ClientePrecioEspecialRequest;
import pe.com.proveperu.sgc.cliente.api.dto.ClientePrecioEspecialResponse;
import pe.com.proveperu.sgc.cliente.application.service.ClientePrecioEspecialService;
import pe.com.proveperu.sgc.cliente.application.service.PermisosCliente;
import pe.com.proveperu.sgc.config.OpenApiConfig;

@RestController
@RequestMapping("/api/v1/clientes/{idCliente}/precios-especiales")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Gestión de personas naturales y jurídicas")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ClientePrecioEspecialController {

    private final ClientePrecioEspecialService precioEspecialService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisosCliente.PRECIOS_VER + "')")
    @Operation(summary = "Listar los precios especiales de un cliente")
    public List<ClientePrecioEspecialResponse> listar(@PathVariable Long idCliente) {
        return precioEspecialService.listar(idCliente);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermisosCliente.PRECIOS_CREAR + "')")
    @Operation(summary = "Registrar un precio especial para un cliente")
    public ResponseEntity<ClientePrecioEspecialResponse> crear(
        @PathVariable Long idCliente,
        @Valid @RequestBody ClientePrecioEspecialRequest request
    ) {
        ClientePrecioEspecialResponse precio = precioEspecialService.crear(idCliente, request);
        return ResponseEntity.created(URI.create(
            "/api/v1/clientes/" + idCliente + "/precios-especiales/" + precio.id()
        )).body(precio);
    }
}
