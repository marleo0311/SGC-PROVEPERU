package pe.com.proveperu.sgc.reporte.application.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.proveperu.sgc.inventario.domain.model.Sede;
import pe.com.proveperu.sgc.inventario.infrastructure.persistence.SedeRepository;
import pe.com.proveperu.sgc.reporte.api.dto.PeriodoReporteResponse;
import pe.com.proveperu.sgc.reporte.api.dto.ReporteCajaResponse;
import pe.com.proveperu.sgc.reporte.api.dto.ReporteDashboardResponse;
import pe.com.proveperu.sgc.reporte.api.dto.ReporteFinanzasResponse;
import pe.com.proveperu.sgc.reporte.api.dto.ReporteInventarioResponse;
import pe.com.proveperu.sgc.reporte.api.dto.ReporteVentasResponse;
import pe.com.proveperu.sgc.reporte.infrastructure.persistence.ReporteConsultaRepository;
import pe.com.proveperu.sgc.security.application.exception.RecursoNoEncontradoException;
import pe.com.proveperu.sgc.shared.application.exception.SolicitudInvalidaException;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Lima");

    private final ReporteConsultaRepository reporteRepository;
    private final SedeRepository sedeRepository;

    @Transactional(readOnly = true)
    public ReporteDashboardResponse obtenerDashboard(
        LocalDate desde,
        LocalDate hasta,
        Long idSede
    ) {
        ContextoPeriodo contexto = resolverPeriodo(desde, hasta, idSede);
        ReporteFinanzasResponse finanzas = reporteRepository.consultarFinanzas(
            hoy()
        );
        return new ReporteDashboardResponse(
            Instant.now(),
            contexto.periodo(),
            reporteRepository.consultarResumenVentas(
                contexto.desdeInstant(),
                contexto.hastaExclusivo(),
                contexto.sede().getId()
            ),
            reporteRepository.consultarResumenInventario(
                contexto.sede().getId()
            ),
            finanzas.cuentasCobrar(),
            finanzas.cuentasPagar(),
            reporteRepository.consultarResumenCaja(
                contexto.desdeInstant(),
                contexto.hastaExclusivo(),
                contexto.sede().getId()
            )
        );
    }

    @Transactional(readOnly = true)
    public ReporteVentasResponse obtenerVentas(
        LocalDate desde,
        LocalDate hasta,
        Long idSede,
        int limite
    ) {
        ContextoPeriodo contexto = resolverPeriodo(desde, hasta, idSede);
        return new ReporteVentasResponse(
            contexto.periodo(),
            reporteRepository.consultarResumenVentas(
                contexto.desdeInstant(),
                contexto.hastaExclusivo(),
                contexto.sede().getId()
            ),
            reporteRepository.consultarVentasDiarias(
                contexto.desdeInstant(),
                contexto.hastaExclusivo(),
                contexto.sede().getId()
            ),
            reporteRepository.consultarVentasPorVendedor(
                contexto.desdeInstant(),
                contexto.hastaExclusivo(),
                contexto.sede().getId(),
                limite
            ),
            reporteRepository.consultarProductosMasVendidos(
                contexto.desdeInstant(),
                contexto.hastaExclusivo(),
                contexto.sede().getId(),
                limite
            )
        );
    }

    @Transactional(readOnly = true)
    public ReporteInventarioResponse obtenerInventario(
        Long idSede,
        int limite
    ) {
        Sede sede = resolverSede(idSede);
        return new ReporteInventarioResponse(
            sede.getId(),
            sede.getNombre(),
            reporteRepository.consultarResumenInventario(sede.getId()),
            reporteRepository.consultarProductosStockBajo(
                sede.getId(),
                limite
            )
        );
    }

    @Transactional(readOnly = true)
    public ReporteFinanzasResponse obtenerFinanzas() {
        return reporteRepository.consultarFinanzas(hoy());
    }

    @Transactional(readOnly = true)
    public ReporteCajaResponse obtenerCaja(
        LocalDate desde,
        LocalDate hasta,
        Long idSede
    ) {
        ContextoPeriodo contexto = resolverPeriodo(desde, hasta, idSede);
        return new ReporteCajaResponse(
            contexto.periodo(),
            reporteRepository.consultarResumenCaja(
                contexto.desdeInstant(),
                contexto.hastaExclusivo(),
                contexto.sede().getId()
            ),
            reporteRepository.consultarCajaPorMetodoPago(
                contexto.desdeInstant(),
                contexto.hastaExclusivo(),
                contexto.sede().getId()
            )
        );
    }

    private ContextoPeriodo resolverPeriodo(
        LocalDate desde,
        LocalDate hasta,
        Long idSede
    ) {
        LocalDate hastaEfectiva = hasta == null ? hoy() : hasta;
        LocalDate desdeEfectiva = desde == null
            ? hastaEfectiva.withDayOfMonth(1)
            : desde;
        if (desdeEfectiva.isAfter(hastaEfectiva)) {
            throw new SolicitudInvalidaException(
                "La fecha inicial no puede ser posterior a la fecha final"
            );
        }
        Sede sede = resolverSede(idSede);
        PeriodoReporteResponse periodo = new PeriodoReporteResponse(
            desdeEfectiva,
            hastaEfectiva,
            sede.getId(),
            sede.getNombre()
        );
        return new ContextoPeriodo(
            sede,
            periodo,
            desdeEfectiva.atStartOfDay(ZONA_NEGOCIO).toInstant(),
            hastaEfectiva.plusDays(1).atStartOfDay(ZONA_NEGOCIO).toInstant()
        );
    }

    private Sede resolverSede(Long idSede) {
        if (idSede == null) {
            return sedeRepository
                .findFirstByEstadoIgnoreCaseOrderByIdAsc("ACTIVO")
                .orElseThrow(() -> new RecursoNoEncontradoException(
                    "No existe una sede activa para generar el reporte"
                ));
        }
        return sedeRepository.findById(idSede)
            .orElseThrow(() -> new RecursoNoEncontradoException(
                "No existe la sede solicitada"
            ));
    }

    private LocalDate hoy() {
        return LocalDate.now(ZONA_NEGOCIO);
    }

    private record ContextoPeriodo(
        Sede sede,
        PeriodoReporteResponse periodo,
        Instant desdeInstant,
        Instant hastaExclusivo
    ) {
    }
}
