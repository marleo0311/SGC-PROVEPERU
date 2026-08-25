package pe.com.proveperu.sgc.cuentacobrar.api.controller;

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
import pe.com.proveperu.sgc.cuentacobrar.api.dto.CuentaCobrarDetalleResponse;
import pe.com.proveperu.sgc.cuentacobrar.api.dto.CuentaCobrarResumenResponse;
import pe.com.proveperu.sgc.cuentacobrar.api.dto.CuentaCobrarSaldoInicialRequest;
import pe.com.proveperu.sgc.cuentacobrar.api.dto.CuentaCobrarVencimientoRequest;
import pe.com.proveperu.sgc.cuentacobrar.api.dto.MetodoPagoCobranzaResponse;
import pe.com.proveperu.sgc.cuentacobrar.api.dto.PagoClienteRequest;
import pe.com.proveperu.sgc.cuentacobrar.application.service.CuentaCobrarService;
import pe.com.proveperu.sgc.cuentacobrar.application.service.PermisosCuentaCobrar;
import pe.com.proveperu.sgc.shared.api.dto.PaginaResponse;
import pe.com.proveperu.sgc.venta.domain.model.EstadoCuentaCobrar;

@RestController
@RequestMapping("/api/v1/cuentas-cobrar")
@RequiredArgsConstructor
@Validated
@Tag(
    name = "Cuentas por cobrar",
    description = "Saldos de clientes, vencimientos e historial de cobros parciales o totales"
)
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class CuentaCobrarController {

    private final CuentaCobrarService cuentaCobrarService;

    @PostMapping("/saldos-iniciales")
    @PreAuthorize("hasAuthority('" + PermisosCuentaCobrar.SALDOS_CREAR + "')")
    @Operation(
        summary = "Registrar un saldo inicial de cliente",
        description = "Crea una cuenta histórica sin generar venta, movimiento de inventario, movimiento de caja ni comprobante SUNAT"
    )
    public ResponseEntity<CuentaCobrarResumenResponse> registrarSaldoInicial(
        @Valid @RequestBody CuentaCobrarSaldoInicialRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        CuentaCobrarResumenResponse cuenta = cuentaCobrarService
            .registrarSaldoInicial(request, jwt.getSubject());
        return ResponseEntity.created(URI.create(
            "/api/v1/cuentas-cobrar/" + cuenta.id()
        )).body(cuenta);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisosCuentaCobrar.CUENTAS_VER + "')")
    @Operation(summary = "Consultar cuentas pendientes, pagadas o vencidas")
    public PaginaResponse<CuentaCobrarResumenResponse> listar(
        @RequestParam(required = false) @Positive Long idCliente,
        @RequestParam(required = false) EstadoCuentaCobrar estado,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate desdeVencimiento,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate hastaVencimiento,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return cuentaCobrarService.listar(
            idCliente,
            estado,
            desdeVencimiento,
            hastaVencimiento,
            pagina(page, size)
        );
    }

    @GetMapping("/vencidas")
    @PreAuthorize("hasAuthority('" + PermisosCuentaCobrar.CUENTAS_VER + "')")
    @Operation(summary = "Consultar cuentas vencidas con saldo pendiente")
    public PaginaResponse<CuentaCobrarResumenResponse> listarVencidas(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return cuentaCobrarService.listarVencidas(pagina(page, size));
    }

    @GetMapping("/metodos-pago")
    @PreAuthorize("hasAnyAuthority('" + PermisosCuentaCobrar.CUENTAS_VER + "','"
        + PermisosCuentaCobrar.PAGOS_CREAR + "')")
    @Operation(summary = "Listar métodos de pago activos para cobranzas")
    public List<MetodoPagoCobranzaResponse> listarMetodosPago() {
        return cuentaCobrarService.listarMetodosPago();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisosCuentaCobrar.CUENTAS_VER + "')")
    @Operation(summary = "Consultar una cuenta y todo su historial de pagos")
    public CuentaCobrarDetalleResponse obtener(@PathVariable @Positive Long id) {
        return cuentaCobrarService.obtener(id);
    }

    @PatchMapping("/{id}/vencimiento")
    @PreAuthorize("hasAuthority('" + PermisosCuentaCobrar.CUENTAS_EDITAR + "')")
    @Operation(summary = "Configurar la fecha de vencimiento de una cuenta")
    public CuentaCobrarResumenResponse actualizarVencimiento(
        @PathVariable @Positive Long id,
        @Valid @RequestBody CuentaCobrarVencimientoRequest request
    ) {
        return cuentaCobrarService.actualizarVencimiento(
            id,
            request.fechaVencimiento()
        );
    }

    @PostMapping("/{id}/pagos")
    @PreAuthorize("hasAuthority('" + PermisosCuentaCobrar.PAGOS_CREAR + "')")
    @Operation(
        summary = "Registrar un cobro parcial o total",
        description = "Requiere una caja abierta; bloquea la cuenta, reduce el saldo y registra el pago y su movimiento de caja en la misma transacción"
    )
    public ResponseEntity<CuentaCobrarDetalleResponse> registrarPago(
        @PathVariable @Positive Long id,
        @Valid @RequestBody PagoClienteRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        CuentaCobrarDetalleResponse cuenta = cuentaCobrarService.registrarPago(
            id,
            request,
            jwt.getSubject()
        );
        return ResponseEntity.created(URI.create("/api/v1/cuentas-cobrar/" + id))
            .body(cuenta);
    }

    private PageRequest pagina(int page, int size) {
        return PageRequest.of(
            page,
            size,
            Sort.by(
                Sort.Order.asc("fechaVencimiento").nullsLast(),
                Sort.Order.desc("id")
            )
        );
    }
}
