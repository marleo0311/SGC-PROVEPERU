package pe.com.proveperu.sgc.pedido.api.controller;

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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.proveperu.sgc.config.OpenApiConfig;
import pe.com.proveperu.sgc.cotizacion.application.service.PermisosCotizacion;
import pe.com.proveperu.sgc.pedido.api.dto.PedidoEstadoRequest;
import pe.com.proveperu.sgc.pedido.api.dto.PedidoGuardarRequest;
import pe.com.proveperu.sgc.pedido.api.dto.PedidoResponse;
import pe.com.proveperu.sgc.pedido.api.dto.PedidoResumenResponse;
import pe.com.proveperu.sgc.pedido.api.dto.ReservaStockResponse;
import pe.com.proveperu.sgc.pedido.application.service.PedidoService;
import pe.com.proveperu.sgc.pedido.application.service.PermisosPedido;
import pe.com.proveperu.sgc.pedido.domain.model.CanalPedido;
import pe.com.proveperu.sgc.pedido.domain.model.EstadoPedido;
import pe.com.proveperu.sgc.shared.api.dto.PaginaResponse;
import pe.com.proveperu.sgc.venta.application.service.PermisosVenta;

@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
@Validated
@Tag(
    name = "Pedidos",
    description = "Pedidos presenciales o de WhatsApp, reservas y preparación"
)
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class PedidoController {

    private final PedidoService pedidoService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('" + PermisosPedido.PEDIDOS_VER + "', '"
        + PermisosVenta.VENTAS_CREAR + "')")
    @Operation(summary = "Listar pedidos con filtros comerciales y operativos")
    public PaginaResponse<PedidoResumenResponse> listar(
        @RequestParam(required = false) @Positive Long idCliente,
        @RequestParam(required = false) CanalPedido canal,
        @RequestParam(required = false) EstadoPedido estado,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate desde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate hasta,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return pedidoService.listar(
            idCliente,
            canal,
            estado,
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
    @PreAuthorize("hasAuthority('" + PermisosPedido.PEDIDOS_VER + "')")
    @Operation(summary = "Consultar el detalle y las reservas de un pedido")
    public PedidoResponse obtener(@PathVariable @Positive Long id) {
        return pedidoService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermisosPedido.PEDIDOS_CREAR + "')")
    @Operation(
        summary = "Registrar un pedido presencial o recibido por WhatsApp",
        description = "Resuelve precios y cantidades, pero no reserva stock hasta confirmar"
    )
    public ResponseEntity<PedidoResponse> crear(
        @Valid @RequestBody PedidoGuardarRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        PedidoResponse pedido = pedidoService.crear(
            request,
            jwt.getSubject(),
            tienePermisoDescuento(jwt)
        );
        Long id = pedido.pedido().id();
        return ResponseEntity.created(URI.create("/api/v1/pedidos/" + id))
            .body(pedido);
    }

    @PostMapping("/{id}/confirmar")
    @PreAuthorize("hasAuthority('" + PermisosPedido.PEDIDOS_CONFIRMAR + "')")
    @Operation(
        summary = "Confirmar un pedido y reservar stock",
        description = "La confirmación es transaccional y falla completamente si un producto no tiene disponibilidad"
    )
    public PedidoResponse confirmar(
        @PathVariable @Positive Long id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return pedidoService.confirmar(id, jwt.getSubject());
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAuthority('" + PermisosPedido.PEDIDOS_CANCELAR + "')")
    @Operation(summary = "Cancelar un pedido y liberar sus reservas activas")
    public PedidoResponse cancelar(
        @PathVariable @Positive Long id,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return pedidoService.cancelar(id, jwt.getSubject());
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('" + PermisosPedido.PEDIDOS_ESTADO + "')")
    @Operation(
        summary = "Actualizar el estado de cotización, pago o preparación",
        description = "ENTREGADO no se admite aquí: la entrega se registra al convertir el pedido en una venta"
    )
    public PedidoResponse cambiarEstado(
        @PathVariable @Positive Long id,
        @Valid @RequestBody PedidoEstadoRequest request
    ) {
        return pedidoService.cambiarEstado(id, request.estado());
    }

    @GetMapping("/{id}/reservas")
    @PreAuthorize("hasAuthority('" + PermisosPedido.RESERVAS_VER + "')")
    @Operation(summary = "Consultar las reservas asociadas a un pedido")
    public List<ReservaStockResponse> listarReservas(
        @PathVariable @Positive Long id
    ) {
        return pedidoService.listarReservas(id);
    }

    private boolean tienePermisoDescuento(Jwt jwt) {
        List<String> authorities = jwt.getClaimAsStringList("authorities");
        return authorities != null
            && authorities.contains(PermisosCotizacion.DESCUENTOS_APLICAR);
    }
}
