package pe.com.proveperu.sgc.catalogo.api.dto;

import java.math.BigDecimal;
import pe.com.proveperu.sgc.catalogo.domain.model.PresentacionProducto;

public record PresentacionProductoResponse(
    Long id,
    Long idProducto,
    String producto,
    UnidadMedidaResponse unidadPresentacion,
    UnidadMedidaResponse unidadBase,
    String nombre,
    boolean contenidoVariable,
    BigDecimal contenidoBasePredeterminado,
    BigDecimal precioMinorista,
    BigDecimal precioMayorista,
    String estado
) {
    public static PresentacionProductoResponse from(PresentacionProducto presentacion) {
        return new PresentacionProductoResponse(
            presentacion.getId(),
            presentacion.getProducto().getId(),
            presentacion.getProducto().getNombre(),
            UnidadMedidaResponse.from(presentacion.getUnidadMedida()),
            UnidadMedidaResponse.from(presentacion.getProducto().getUnidadBase()),
            presentacion.getNombre(),
            presentacion.isContenidoVariable(),
            presentacion.getContenidoBasePredeterminado(),
            presentacion.getPrecioMinorista(),
            presentacion.getPrecioMayorista(),
            presentacion.getEstado().name()
        );
    }
}
