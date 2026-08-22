package pe.com.proveperu.sgc.devolucion.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import java.time.LocalDate;
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
import pe.com.proveperu.sgc.devolucion.api.dto.DevolucionCrearRequest;
import pe.com.proveperu.sgc.devolucion.api.dto.DevolucionResponse;
import pe.com.proveperu.sgc.devolucion.api.dto.DevolucionResumenResponse;
import pe.com.proveperu.sgc.devolucion.api.dto.ReembolsoDevolucionRequest;
import pe.com.proveperu.sgc.devolucion.application.service.DevolucionService;
import pe.com.proveperu.sgc.devolucion.application.service.PermisosDevolucion;
import pe.com.proveperu.sgc.devolucion.domain.model.EstadoDevolucion;
import pe.com.proveperu.sgc.devolucion.domain.model.TipoSolucionDevolucion;
import pe.com.proveperu.sgc.shared.api.dto.PaginaResponse;

@RestController
@RequestMapping("/api/v1/devoluciones")
@RequiredArgsConstructor
@Validated
@Tag(
    name = "Devoluciones",
    description = "Devoluciones de ventas, clasificación del producto y reembolsos en caja"
)
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class DevolucionController {

    private final DevolucionService devolucionService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisosDevolucion.DEVOLUCIONES_VER + "')")
    @Operation(summary = "Listar devoluciones con filtros")
    public PaginaResponse<DevolucionResumenResponse> listar(
        @RequestParam(required = false) @Positive Long idVenta,
        @RequestParam(required = false) EstadoDevolucion estado,
        @RequestParam(required = false) TipoSolucionDevolucion tipoSolucion,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate desde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate hasta,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        return devolucionService.listar(
            idVenta,
            estado,
            tipoSolucion,
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
    @PreAuthorize("hasAuthority('" + PermisosDevolucion.DEVOLUCIONES_VER + "')")
    @Operation(summary = "Consultar una devolución, sus productos y reembolso")
    public DevolucionResponse obtener(@PathVariable @Positive Long id) {
        return devolucionService.obtener(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermisosDevolucion.DEVOLUCIONES_CREAR + "')")
    @Operation(
        summary = "Registrar una devolución asociada a una venta",
        description = "Valida cantidades previamente devueltas, clasifica cada producto, reincorpora solo los aptos y ajusta el saldo por cobrar"
    )
    public ResponseEntity<DevolucionResponse> registrar(
        @Valid @RequestBody DevolucionCrearRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        DevolucionResponse devolucion = devolucionService.registrar(
            request,
            jwt.getSubject()
        );
        Long id = devolucion.devolucion().id();
        return ResponseEntity.created(URI.create("/api/v1/devoluciones/" + id))
            .body(devolucion);
    }

    @PostMapping("/{id}/reembolso")
    @PreAuthorize("hasAuthority('" + PermisosDevolucion.REEMBOLSOS_CREAR + "')")
    @Operation(
        summary = "Registrar la devolución de dinero",
        description = "Exige caja abierta y registra de forma transaccional el reembolso, el egreso de caja y el ajuste de la cuenta por cobrar"
    )
    public ResponseEntity<DevolucionResponse> reembolsar(
        @PathVariable @Positive Long id,
        @Valid @RequestBody ReembolsoDevolucionRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        DevolucionResponse devolucion = devolucionService.reembolsar(
            id,
            request,
            jwt.getSubject()
        );
        return ResponseEntity.created(URI.create(
            "/api/v1/devoluciones/" + id + "/reembolso"
        )).body(devolucion);
    }
}
