package pe.com.proveperu.sgc.cotizacion.api.dto;

import java.time.Instant;
import java.util.List;

public record CotizacionResponse(
    CotizacionResumenResponse cotizacion,
    Long idSedeConsulta,
    String sedeConsulta,
    boolean todosDisponibles,
    List<CotizacionDetalleResponse> detalles,
    Instant fechaRegistro,
    Instant fechaActualizacion
) {
}
