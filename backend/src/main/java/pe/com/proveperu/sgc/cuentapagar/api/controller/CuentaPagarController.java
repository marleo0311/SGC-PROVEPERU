package pe.com.proveperu.sgc.cuentapagar.api.controller;

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
import pe.com.proveperu.sgc.cuentapagar.api.dto.CuentaPagarDetalleResponse;
import pe.com.proveperu.sgc.cuentapagar.api.dto.CuentaPagarResumenResponse;
import pe.com.proveperu.sgc.cuentapagar.api.dto.CuentaPagarVencimientoRequest;
import pe.com.proveperu.sgc.cuentapagar.api.dto.MetodoPagoResponse;
import pe.com.proveperu.sgc.cuentapagar.api.dto.PagoProveedorRequest;
import pe.com.proveperu.sgc.cuentapagar.application.service.CuentaPagarService;
import pe.com.proveperu.sgc.cuentapagar.application.service.PermisosCuentaPagar;
import pe.com.proveperu.sgc.cuentapagar.domain.model.EstadoCuentaPagar;
import pe.com.proveperu.sgc.shared.api.dto.PaginaResponse;

@RestController
@RequestMapping("/api/v1/cuentas-pagar")
@RequiredArgsConstructor
@Validated
@Tag(
    name = "Cuentas por pagar",
    description = "Deudas con proveedores, vencimientos y pagos parciales o totales"
)
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class CuentaPagarController {

    private final CuentaPagarService cuentaPagarService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisosCuentaPagar.CUENTAS_VER + "')")
    @Operation(summary = "Consultar cuentas pendientes, pagadas o vencidas")
    public PaginaResponse<CuentaPagarResumenResponse> listar(
        @RequestParam(required = false) @Positive Long idProveedor,
        @RequestParam(required = false) EstadoCuentaPagar estado,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate desdeVencimiento,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate hastaVencimiento,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return cuentaPagarService.listar(
            idProveedor,
            estado,
            desdeVencimiento,
            hastaVencimiento,
            pagina(page, size)
        );
    }

    @GetMapping("/vencidas")
    @PreAuthorize("hasAuthority('" + PermisosCuentaPagar.CUENTAS_VER + "')")
    @Operation(summary = "Consultar obligaciones vencidas con saldo pendiente")
    public PaginaResponse<CuentaPagarResumenResponse> listarVencidas(
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return cuentaPagarService.listarVencidas(pagina(page, size));
    }

    @GetMapping("/metodos-pago")
    @PreAuthorize("hasAuthority('" + PermisosCuentaPagar.CUENTAS_VER + "')")
    @Operation(summary = "Listar los métodos de pago activos disponibles")
    public List<MetodoPagoResponse> listarMetodosPago() {
        return cuentaPagarService.listarMetodosPago();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('" + PermisosCuentaPagar.CUENTAS_VER + "')")
    @Operation(summary = "Consultar una cuenta por pagar y su historial de pagos")
    public CuentaPagarDetalleResponse obtener(@PathVariable @Positive Long id) {
        return cuentaPagarService.obtener(id);
    }

    @PatchMapping("/{id}/vencimiento")
    @PreAuthorize("hasAuthority('" + PermisosCuentaPagar.CUENTAS_EDITAR + "')")
    @Operation(summary = "Configurar la fecha de vencimiento de una cuenta")
    public CuentaPagarResumenResponse actualizarVencimiento(
        @PathVariable @Positive Long id,
        @Valid @RequestBody CuentaPagarVencimientoRequest request
    ) {
        return cuentaPagarService.actualizarVencimiento(
            id,
            request.fechaVencimiento()
        );
    }

    @PostMapping("/{id}/pagos")
    @PreAuthorize("hasAuthority('" + PermisosCuentaPagar.PAGOS_CREAR + "')")
    @Operation(
        summary = "Registrar un pago parcial o total",
        description = "Reduce el saldo sin eliminar pagos anteriores y conserva el usuario responsable"
    )
    public ResponseEntity<CuentaPagarDetalleResponse> registrarPago(
        @PathVariable @Positive Long id,
        @Valid @RequestBody PagoProveedorRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        CuentaPagarDetalleResponse cuenta = cuentaPagarService.registrarPago(
            id,
            request,
            jwt.getSubject()
        );
        return ResponseEntity.created(URI.create("/api/v1/cuentas-pagar/" + id))
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
