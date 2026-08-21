package pe.com.proveperu.sgc.catalogo.api.controller;

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
import pe.com.proveperu.sgc.catalogo.api.dto.ConversionCrearRequest;
import pe.com.proveperu.sgc.catalogo.api.dto.ConversionResponse;
import pe.com.proveperu.sgc.catalogo.application.service.ConversionProductoService;
import pe.com.proveperu.sgc.catalogo.application.service.PermisosCatalogo;
import pe.com.proveperu.sgc.config.OpenApiConfig;

@RestController
@RequestMapping("/api/v1/productos/{idProducto}/conversiones")
@RequiredArgsConstructor
@Tag(name = "Conversiones de producto", description = "Conversiones para productos fraccionables")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ProductoConversionController {

    private final ConversionProductoService conversionProductoService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisosCatalogo.CONVERSIONES_VER + "')")
    @Operation(summary = "Listar conversiones de un producto")
    public List<ConversionResponse> listar(@PathVariable Long idProducto) {
        return conversionProductoService.listar(idProducto);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermisosCatalogo.CONVERSIONES_CREAR + "')")
    @Operation(summary = "Crear una conversión de unidades")
    public ResponseEntity<ConversionResponse> crear(
        @PathVariable Long idProducto,
        @Valid @RequestBody ConversionCrearRequest request
    ) {
        ConversionResponse conversion = conversionProductoService.crear(idProducto, request);
        URI location = URI.create(
            "/api/v1/productos/" + idProducto + "/conversiones/" + conversion.id()
        );
        return ResponseEntity.created(location).body(conversion);
    }
}
