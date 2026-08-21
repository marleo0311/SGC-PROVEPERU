package pe.com.proveperu.sgc.catalogo.api.dto;

import java.math.BigDecimal;
import pe.com.proveperu.sgc.catalogo.domain.model.ProductoUnidadConversion;

public record ConversionResponse(
    Long id,
    Long idProducto,
    UnidadMedidaResponse unidadOrigen,
    UnidadMedidaResponse unidadDestino,
    BigDecimal factorConversion,
    String estado
) {
    public static ConversionResponse from(ProductoUnidadConversion conversion) {
        return new ConversionResponse(
            conversion.getId(),
            conversion.getProducto().getId(),
            UnidadMedidaResponse.from(conversion.getUnidadOrigen()),
            UnidadMedidaResponse.from(conversion.getUnidadDestino()),
            conversion.getFactorConversion(),
            conversion.getEstado().name()
        );
    }
}
