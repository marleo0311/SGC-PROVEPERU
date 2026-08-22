package pe.com.proveperu.sgc.inventario.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.proveperu.sgc.config.OpenApiConfig;
import pe.com.proveperu.sgc.inventario.api.dto.MovimientoInventarioResponse;
import pe.com.proveperu.sgc.inventario.application.service.InventarioService;
import pe.com.proveperu.sgc.inventario.application.service.PermisosInventario;
import pe.com.proveperu.sgc.inventario.domain.model.TipoMovimientoInventario;
import pe.com.proveperu.sgc.shared.api.dto.PaginaResponse;

@RestController
@RequestMapping("/api/v1/kardex")
@RequiredArgsConstructor
@Validated
@Tag(name = "Kardex", description = "Historial cronológico de movimientos por producto")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class KardexController {

    private final InventarioService inventarioService;

    @GetMapping("/{idProducto}")
    @PreAuthorize("hasAuthority('" + PermisosInventario.KARDEX_VER + "')")
    @Operation(summary = "Consultar Kardex cronológico de un producto")
    public PaginaResponse<MovimientoInventarioResponse> consultar(
        @PathVariable @Positive Long idProducto,
        @RequestParam(required = false) @Positive Long idSede,
        @RequestParam(required = false) TipoMovimientoInventario tipo,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate desde,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate hasta,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size
    ) {
        PageRequest pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Order.asc("fechaHora"), Sort.Order.asc("id"))
        );
        return inventarioService.consultarKardex(
            idProducto,
            idSede,
            tipo,
            desde,
            hasta,
            pageable
        );
    }
}
