package pe.com.proveperu.sgc.catalogo.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import pe.com.proveperu.sgc.catalogo.domain.model.PrecioProducto;

public record PrecioResponse(
    Long id,
    Long idProducto,
    String tipoPrecio,
    BigDecimal monto,
    LocalDate vigenteDesde,
    LocalDate vigenteHasta,
    String estado
) {
    public static PrecioResponse from(PrecioProducto precio) {
        return new PrecioResponse(
            precio.getId(),
            precio.getProducto().getId(),
            precio.getTipoPrecio(),
            precio.getMonto(),
            precio.getVigenteDesde(),
            precio.getVigenteHasta(),
            precio.getEstado().name()
        );
    }
}
