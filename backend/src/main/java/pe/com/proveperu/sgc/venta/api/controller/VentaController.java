package pe.com.proveperu.sgc.venta.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.proveperu.sgc.config.OpenApiConfig;
import pe.com.proveperu.sgc.shared.api.dto.PaginaResponse;
import pe.com.proveperu.sgc.venta.api.dto.MetodoPagoVentaResponse;
import pe.com.proveperu.sgc.venta.api.dto.VentaAnularRequest;
import pe.com.proveperu.sgc.venta.api.dto.VentaCrearRequest;
import pe.com.proveperu.sgc.venta.api.dto.VentaResponse;
import pe.com.proveperu.sgc.venta.api.dto.VentaResumenResponse;
import pe.com.proveperu.sgc.venta.application.service.PermisosVenta;
import pe.com.proveperu.sgc.venta.application.service.VentaService;
import pe.com.proveperu.sgc.venta.domain.model.CondicionPagoVenta;
import pe.com.proveperu.sgc.venta.domain.model.EstadoVenta;

@RestController
@RequestMapping("/api/v1/ventas")
@RequiredArgsConstructor
@Validated
@Tag(
    name = "Ventas",
    description = "Ventas directas o desde pedidos, pagos iniciales y salida de inventario"
)
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class VentaController {

    private final VentaService ventaService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + PermisosVenta.VENTAS_VER + "', '"
        + PermisosVenta.COMPROBANTES_VER + "')")
    @Operation(summary = "Listar ventas con filtros comerciales")
    public PaginaResponse<VentaResumenResponse> listar(
        @RequestParam(required = false) @Positive Long idCliente,
        @RequestParam(required = false) EstadoVenta estado,
        @RequestParam(required = false) CondicionPagoVenta condicionPago,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate desde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate hasta,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return ventaService.listar(
            idCliente,
            estado,
            condicionPago,
            desde,
            hasta,
            PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("fechaHora"), Sort.Order.desc("id"))
            )
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('" + PermisosVenta.VENTAS_VER + "', '"
        + PermisosVenta.COMPROBANTES_VER + "')")
    @Operation(summary = "Consultar detalle, pago inicial y saldo de una venta")
    public VentaResponse obtener(@PathVariable @Positive Long id) {
        return ventaService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermisosVenta.VENTAS_CREAR + "')")
    @Operation(
        summary = "Registrar y confirmar una venta",
        description = "Valida precios, stock y caja abierta; en la misma transacción descuenta inventario, registra Kardex, pago, movimiento de caja y saldo por cobrar"
    )
    public ResponseEntity<VentaResponse> crear(
        @Valid @RequestBody VentaCrearRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        VentaResponse venta = ventaService.crear(
            request,
            jwt.getSubject(),
            tienePermisoDescuento(jwt)
        );
        Long id = venta.venta().id();
        return ResponseEntity.created(URI.create("/api/v1/ventas/" + id))
            .body(venta);
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasAnyAuthority('" + PermisosVenta.VENTAS_ANULAR + "', '"
        + PermisosVenta.COMPROBANTES_ANULAR + "')")
    @Operation(
        summary = "Anular una venta y reponer su inventario",
        description = "Conserva los pagos como historial y deja sin saldo exigible la cuenta por cobrar"
    )
    public VentaResponse anular(
        @PathVariable @Positive Long id,
        @Valid @RequestBody VentaAnularRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return ventaService.anular(id, request, jwt.getSubject());
    }

    @GetMapping("/metodos-pago")
    @PreAuthorize("hasAnyAuthority('" + PermisosVenta.VENTAS_VER + "','"
        + PermisosVenta.VENTAS_CREAR + "')")
    @Operation(summary = "Listar métodos de pago activos disponibles para ventas")
    public List<MetodoPagoVentaResponse> listarMetodosPago() {
        return ventaService.listarMetodosPago();
    }

    private boolean tienePermisoDescuento(Jwt jwt) {
        List<String> authorities = jwt.getClaimAsStringList("authorities");
        return authorities != null
            && authorities.contains(PermisosVenta.DESCUENTOS_APLICAR);
    }
}
