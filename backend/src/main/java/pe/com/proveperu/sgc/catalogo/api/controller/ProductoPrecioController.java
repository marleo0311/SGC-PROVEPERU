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
import pe.com.proveperu.sgc.catalogo.api.dto.PrecioCrearRequest;
import pe.com.proveperu.sgc.catalogo.api.dto.PrecioResponse;
import pe.com.proveperu.sgc.catalogo.application.service.PermisosCatalogo;
import pe.com.proveperu.sgc.catalogo.application.service.PrecioProductoService;
import pe.com.proveperu.sgc.config.OpenApiConfig;
import pe.com.proveperu.sgc.cotizacion.application.service.PermisosCotizacion;
import pe.com.proveperu.sgc.pedido.application.service.PermisosPedido;
import pe.com.proveperu.sgc.venta.application.service.PermisosVenta;

@RestController
@RequestMapping("/api/v1/productos/{idProducto}/precios")
@RequiredArgsConstructor
@Tag(name = "Precios de producto", description = "Historial y vigencias de precios")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class ProductoPrecioController {

    private final PrecioProductoService precioProductoService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + PermisosCatalogo.PRECIOS_VER + "', '"
        + PermisosCatalogo.PRODUCTOS_EDITAR + "', '"
        + PermisosCotizacion.COTIZACIONES_CREAR + "', '"
        + PermisosCotizacion.COTIZACIONES_EDITAR + "', '"
        + PermisosPedido.PEDIDOS_CREAR + "', '"
        + PermisosVenta.VENTAS_CREAR + "')")
    @Operation(summary = "Consultar el historial de precios de un producto")
    public List<PrecioResponse> listar(@PathVariable Long idProducto) {
        return precioProductoService.listar(idProducto);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermisosCatalogo.PRECIOS_CREAR + "')")
    @Operation(summary = "Registrar una nueva vigencia de precio")
    public ResponseEntity<PrecioResponse> crear(
        @PathVariable Long idProducto,
        @Valid @RequestBody PrecioCrearRequest request
    ) {
        PrecioResponse precio = precioProductoService.crear(idProducto, request);
        URI location = URI.create("/api/v1/productos/" + idProducto + "/precios/" + precio.id());
        return ResponseEntity.created(location).body(precio);
    }
}
