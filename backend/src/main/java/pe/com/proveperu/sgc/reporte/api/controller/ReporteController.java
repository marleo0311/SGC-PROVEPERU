package pe.com.proveperu.sgc.reporte.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.proveperu.sgc.config.OpenApiConfig;
import pe.com.proveperu.sgc.reporte.api.dto.ReporteCajaResponse;
import pe.com.proveperu.sgc.reporte.api.dto.ReporteDashboardResponse;
import pe.com.proveperu.sgc.reporte.api.dto.ReporteFinanzasResponse;
import pe.com.proveperu.sgc.reporte.api.dto.ReporteInventarioResponse;
import pe.com.proveperu.sgc.reporte.api.dto.ReporteVentasResponse;
import pe.com.proveperu.sgc.reporte.application.service.PermisosReporte;
import pe.com.proveperu.sgc.reporte.application.service.ReporteService;
import pe.com.proveperu.sgc.reporte.application.service.ReporteExportService;

@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
@Validated
@Tag(
    name = "Reportes",
    description = "Dashboard e indicadores consolidados de la gestión comercial"
)
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
@PreAuthorize("hasAuthority('" + PermisosReporte.REPORTES_VER + "')")
public class ReporteController {

    private final ReporteService reporteService;
    private final ReporteExportService reporteExportService;

    @GetMapping("/dashboard")
    @Operation(summary = "Consultar los indicadores principales del dashboard")
    public ReporteDashboardResponse dashboard(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
        @RequestParam(required = false) @Positive Long idSede
    ) {
        return reporteService.obtenerDashboard(desde, hasta, idSede);
    }

    @GetMapping("/ventas")
    @Operation(
        summary = "Consultar ventas por día, vendedor y producto",
        description = "Excluye las ventas anuladas y usa la zona horaria de Lima"
    )
    public ReporteVentasResponse ventas(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
        @RequestParam(required = false) @Positive Long idSede,
        @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limite
    ) {
        return reporteService.obtenerVentas(desde, hasta, idSede, limite);
    }

    @GetMapping("/inventario")
    @Operation(summary = "Consultar existencias bajas y productos agotados")
    public ReporteInventarioResponse inventario(
        @RequestParam(required = false) @Positive Long idSede,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limite
    ) {
        return reporteService.obtenerInventario(idSede, limite);
    }

    @GetMapping("/finanzas")
    @Operation(summary = "Consultar cuentas por cobrar y pagar pendientes")
    public ReporteFinanzasResponse finanzas() {
        return reporteService.obtenerFinanzas();
    }

    @GetMapping("/caja")
    @Operation(summary = "Consultar ingresos y egresos de caja por método de pago")
    public ReporteCajaResponse caja(
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
        @RequestParam(required = false) @Positive Long idSede
    ) {
        return reporteService.obtenerCaja(desde, hasta, idSede);
    }

    @GetMapping("/exportar/{tipo}")
    @Operation(summary = "Exportar un reporte detallado en Excel o PDF")
    public ResponseEntity<byte[]> exportar(
        @org.springframework.web.bind.annotation.PathVariable String tipo,
        @RequestParam(defaultValue = "XLSX") String formato,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
        @RequestParam(required = false) @Positive Long idSede,
        @RequestParam(defaultValue = "50") @Min(1) @Max(50) int limite
    ) {
        var archivo = reporteExportService.exportar(tipo, formato, desde, hasta, idSede, limite);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(archivo.mediaType()))
            .header(
                HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(archivo.nombre()).build().toString()
            )
            .body(archivo.contenido());
    }
}
