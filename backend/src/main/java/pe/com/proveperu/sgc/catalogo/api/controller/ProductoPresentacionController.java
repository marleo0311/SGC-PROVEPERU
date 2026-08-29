package pe.com.proveperu.sgc.catalogo.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
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
import org.springframework.web.bind.annotation.RestController;
import pe.com.proveperu.sgc.catalogo.api.dto.PresentacionProductoEstadoRequest;
import pe.com.proveperu.sgc.catalogo.api.dto.PresentacionProductoGuardarRequest;
import pe.com.proveperu.sgc.catalogo.api.dto.PresentacionProductoResponse;
import pe.com.proveperu.sgc.catalogo.application.service.PermisosCatalogo;
import pe.com.proveperu.sgc.catalogo.application.service.PresentacionProductoService;
import pe.com.proveperu.sgc.config.OpenApiConfig;
import pe.com.proveperu.sgc.compra.application.service.PermisosCompra;
import pe.com.proveperu.sgc.venta.application.service.PermisosVenta;

@RestController
@RequestMapping("/api/v1/productos/{idProducto}/presentaciones")
@RequiredArgsConstructor
@Validated
@Tag(name = "Presentaciones de producto", description = "Cajas, paquetes y rollos por producto")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ProductoPresentacionController {

    private final PresentacionProductoService presentacionService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + PermisosCatalogo.PRESENTACIONES_VER + "', '"
        + PermisosCatalogo.PRODUCTOS_EDITAR + "', '"
        + PermisosVenta.VENTAS_CREAR + "', '"
        + PermisosCompra.COMPRAS_CREAR + "', '"
        + PermisosCompra.COMPRAS_EDITAR + "', '"
        + PermisosCompra.RECEPCIONES_CREAR + "')")
    @Operation(summary = "Listar presentaciones configuradas")
    public List<PresentacionProductoResponse> listar(
        @PathVariable @Positive Long idProducto
    ) {
        return presentacionService.listar(idProducto);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermisosCatalogo.PRESENTACIONES_EDITAR + "')")
    public ResponseEntity<PresentacionProductoResponse> crear(
        @PathVariable @Positive Long idProducto,
        @Valid @RequestBody PresentacionProductoGuardarRequest request
    ) {
        PresentacionProductoResponse response = presentacionService.crear(idProducto, request);
        return ResponseEntity.created(URI.create(
            "/api/v1/productos/" + idProducto + "/presentaciones/" + response.id()
        )).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisosCatalogo.PRESENTACIONES_EDITAR + "')")
    public PresentacionProductoResponse actualizar(
        @PathVariable @Positive Long idProducto,
        @PathVariable @Positive Long id,
        @Valid @RequestBody PresentacionProductoGuardarRequest request
    ) {
        return presentacionService.actualizar(idProducto, id, request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('" + PermisosCatalogo.PRESENTACIONES_EDITAR + "')")
    public PresentacionProductoResponse cambiarEstado(
        @PathVariable @Positive Long idProducto,
        @PathVariable @Positive Long id,
        @Valid @RequestBody PresentacionProductoEstadoRequest request
    ) {
        return presentacionService.cambiarEstado(idProducto, id, request.estado());
    }
}
