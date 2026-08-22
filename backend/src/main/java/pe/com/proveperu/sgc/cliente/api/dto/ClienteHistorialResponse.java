package pe.com.proveperu.sgc.cliente.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ClienteHistorialResponse(
    ClienteResponse cliente,
    Resumen resumen,
    List<ClienteOperacionResponse> operaciones,
    List<ClientePrecioEspecialResponse> preciosEspeciales
) {
    public record Resumen(
        long totalOperaciones,
        BigDecimal importeTotal,
        BigDecimal saldoPendiente,
        Instant ultimaOperacion
    ) {
    }
}
