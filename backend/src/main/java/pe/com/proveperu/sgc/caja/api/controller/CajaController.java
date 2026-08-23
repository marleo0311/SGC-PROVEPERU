package pe.com.proveperu.sgc.caja.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.time.Instant;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.proveperu.sgc.caja.api.dto.AperturaCajaRequest;
import pe.com.proveperu.sgc.caja.api.dto.CajaResponse;
import pe.com.proveperu.sgc.caja.api.dto.CierreCajaRequest;
import pe.com.proveperu.sgc.caja.api.dto.MovimientoCajaRequest;
import pe.com.proveperu.sgc.caja.api.dto.MovimientoCajaResponse;
import pe.com.proveperu.sgc.caja.api.dto.MetodoPagoCajaResponse;
import pe.com.proveperu.sgc.caja.api.dto.ResumenCajaResponse;
import pe.com.proveperu.sgc.caja.api.dto.SesionCajaResponse;
import pe.com.proveperu.sgc.caja.application.service.CajaService;
import pe.com.proveperu.sgc.caja.application.service.PermisosCaja;
import pe.com.proveperu.sgc.caja.domain.model.TipoMovimientoCaja;
import pe.com.proveperu.sgc.config.OpenApiConfig;
import pe.com.proveperu.sgc.shared.api.dto.PaginaResponse;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(
    name = "Caja",
    description = "Apertura, ingresos, egresos, arqueo y cierre de caja"
)
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class CajaController {

    private final CajaService cajaService;

    @GetMapping("/api/v1/cajas")
    @PreAuthorize("hasAuthority('" + PermisosCaja.CAJAS_VER + "')")
    @Operation(summary = "Listar cajas activas por sede")
    public List<CajaResponse> listarCajas() {
        return cajaService.listarCajas();
    }

    @GetMapping("/api/v1/cajas/metodos-pago")
    @PreAuthorize("hasAnyAuthority('" + PermisosCaja.CAJAS_VER + "','"
        + PermisosCaja.MOVIMIENTOS_CREAR + "')")
    @Operation(summary = "Listar métodos de pago activos para movimientos de caja")
    public List<MetodoPagoCajaResponse> listarMetodosPago() {
        return cajaService.listarMetodosPago();
    }

    @PostMapping("/api/v1/cajas/{id}/aperturas")
    @PreAuthorize("hasAuthority('" + PermisosCaja.SESIONES_ABRIR + "')")
    @Operation(
        summary = "Abrir una caja",
        description = "Impide que una caja o un usuario mantengan dos sesiones abiertas"
    )
    public ResponseEntity<SesionCajaResponse> abrir(
        @PathVariable @Positive Long id,
        @Valid @RequestBody AperturaCajaRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        SesionCajaResponse sesion = cajaService.abrir(id, request, jwt.getSubject());
        return ResponseEntity.created(
            URI.create("/api/v1/cajas/" + id + "/sesion-activa")
        ).body(sesion);
    }

    @GetMapping("/api/v1/cajas/{id}/sesion-activa")
    @PreAuthorize("hasAuthority('" + PermisosCaja.CAJAS_VER + "')")
    @Operation(summary = "Consultar la sesión activa de una caja")
    public SesionCajaResponse obtenerSesionActiva(
        @PathVariable @Positive Long id
    ) {
        return cajaService.obtenerSesionActiva(id);
    }

    @PostMapping("/api/v1/sesiones-caja/{id}/movimientos")
    @PreAuthorize("hasAuthority('" + PermisosCaja.MOVIMIENTOS_CREAR + "')")
    @Operation(
        summary = "Registrar un ingreso o egreso manual",
        description = "Los movimientos de ventas y cobranzas se generan automáticamente desde sus operaciones"
    )
    public ResponseEntity<MovimientoCajaResponse> registrarMovimiento(
        @PathVariable @Positive Long id,
        @Valid @RequestBody MovimientoCajaRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        MovimientoCajaResponse movimiento = cajaService
            .registrarMovimientoManual(id, request, jwt.getSubject());
        return ResponseEntity.created(URI.create(
            "/api/v1/sesiones-caja/" + id + "/movimientos/" + movimiento.id()
        )).body(movimiento);
    }

    @GetMapping("/api/v1/sesiones-caja/{id}/movimientos")
    @PreAuthorize("hasAuthority('" + PermisosCaja.MOVIMIENTOS_VER + "')")
    @Operation(summary = "Consultar los movimientos de una sesión con filtros")
    public PaginaResponse<MovimientoCajaResponse> listarMovimientos(
        @PathVariable @Positive Long id,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta,
        @RequestParam(required = false) @Positive Long idUsuario,
        @RequestParam(required = false) TipoMovimientoCaja tipo,
        @RequestParam(required = false) @Positive Long idMetodoPago,
        @RequestParam(required = false) @Positive Long idVendedor,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return cajaService.listarMovimientos(
            id,
            desde,
            hasta,
            idUsuario,
            tipo,
            idMetodoPago,
            idVendedor,
            PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("fechaHora"), Sort.Order.desc("id"))
            )
        );
    }

    @PostMapping("/api/v1/sesiones-caja/{id}/cierre")
    @PreAuthorize("hasAuthority('" + PermisosCaja.SESIONES_CERRAR + "')")
    @Operation(
        summary = "Cerrar una sesión de caja",
        description = "Calcula el efectivo esperado y conserva la diferencia frente al saldo real"
    )
    public SesionCajaResponse cerrar(
        @PathVariable @Positive Long id,
        @Valid @RequestBody CierreCajaRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return cajaService.cerrar(id, request, jwt.getSubject());
    }

    @GetMapping("/api/v1/sesiones-caja/{id}/resumen")
    @PreAuthorize("hasAuthority('" + PermisosCaja.RESUMEN_VER + "')")
    @Operation(summary = "Consultar resumen por método y saldo esperado")
    public ResumenCajaResponse obtenerResumen(@PathVariable @Positive Long id) {
        return cajaService.obtenerResumen(id);
    }
}
