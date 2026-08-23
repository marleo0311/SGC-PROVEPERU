package pe.com.proveperu.sgc.inventario.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.proveperu.sgc.config.OpenApiConfig;
import pe.com.proveperu.sgc.compra.application.service.PermisosCompra;
import pe.com.proveperu.sgc.inventario.api.dto.SedeResponse;
import pe.com.proveperu.sgc.inventario.application.service.PermisosInventario;
import pe.com.proveperu.sgc.inventario.application.service.SedeService;
import pe.com.proveperu.sgc.pedido.application.service.PermisosPedido;
import pe.com.proveperu.sgc.venta.application.service.PermisosVenta;

@RestController
@RequestMapping("/api/v1/sedes")
@RequiredArgsConstructor
@Tag(name = "Sedes", description = "Sedes disponibles para operaciones de inventario")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class SedeController {

    private final SedeService sedeService;

    @GetMapping
    @PreAuthorize(
        "hasAnyAuthority('" + PermisosInventario.STOCK_VER
            + "', '" + PermisosInventario.KARDEX_VER
            + "', '" + PermisosCompra.RECEPCIONES_CREAR
            + "', '" + PermisosPedido.PEDIDOS_CREAR
            + "', '" + PermisosPedido.PEDIDOS_CONVERTIR
            + "', '" + PermisosVenta.VENTAS_CREAR + "')"
    )
    @Operation(summary = "Listar sedes activas")
    public List<SedeResponse> listar() {
        return sedeService.listarActivas();
    }
}
