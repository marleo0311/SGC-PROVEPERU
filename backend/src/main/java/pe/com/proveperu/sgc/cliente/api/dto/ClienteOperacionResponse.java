package pe.com.proveperu.sgc.cliente.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ClienteOperacionResponse(
    String tipoOperacion,
    Long idOperacion,
    String referencia,
    String estado,
    BigDecimal importe,
    Instant fechaHora
) {
}
