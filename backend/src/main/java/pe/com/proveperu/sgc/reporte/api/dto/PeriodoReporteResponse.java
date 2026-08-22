package pe.com.proveperu.sgc.reporte.api.dto;

import java.time.LocalDate;

public record PeriodoReporteResponse(
    LocalDate desde,
    LocalDate hasta,
    Long idSede,
    String nombreSede
) {
}
