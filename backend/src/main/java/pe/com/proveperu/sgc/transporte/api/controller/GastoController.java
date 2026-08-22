package pe.com.proveperu.sgc.transporte.api.controller;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.proveperu.sgc.config.OpenApiConfig;
import pe.com.proveperu.sgc.shared.api.dto.PaginaResponse;
import pe.com.proveperu.sgc.transporte.api.dto.GastoCrearRequest;
import pe.com.proveperu.sgc.transporte.api.dto.GastoResponse;
import pe.com.proveperu.sgc.transporte.application.service.GastoService;
import pe.com.proveperu.sgc.transporte.application.service.PermisosTransporte;
import pe.com.proveperu.sgc.transporte.domain.model.TipoGasto;

@RestController
@RequestMapping("/api/v1/gastos")
@RequiredArgsConstructor
@Validated
@Tag(name = "Gastos", description = "Gastos de transporte y operaciones comerciales")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class GastoController {

    private final GastoService gastoService;

    @GetMapping
    @PreAuthorize("hasAuthority('" + PermisosTransporte.GASTOS_VER + "')")
    @Operation(summary = "Consultar gastos con filtros")
    public PaginaResponse<GastoResponse> listar(
        @RequestParam(required = false) @Positive Long idTransportista,
        @RequestParam(required = false) TipoGasto tipoGasto,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate desde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate hasta,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        PageRequest pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Order.desc("fecha"), Sort.Order.desc("id"))
        );
        return gastoService.listar(idTransportista, tipoGasto, desde, hasta, pageable);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + PermisosTransporte.GASTOS_CREAR + "')")
    @Operation(
        summary = "Registrar un gasto",
        description = "Asocia automáticamente al usuario autenticado como responsable"
    )
    public ResponseEntity<GastoResponse> crear(
        @Valid @RequestBody GastoCrearRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        GastoResponse gasto = gastoService.crear(request, jwt.getSubject());
        return ResponseEntity.created(URI.create("/api/v1/gastos/" + gasto.id())).body(gasto);
    }
}
