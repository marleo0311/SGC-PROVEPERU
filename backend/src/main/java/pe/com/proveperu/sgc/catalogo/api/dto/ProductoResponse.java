package pe.com.proveperu.sgc.catalogo.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import pe.com.proveperu.sgc.catalogo.domain.model.Producto;

public record ProductoResponse(
    Long id,
    String codigoInterno,
    String codigoBarras,
    String nombre,
    String descripcion,
    BigDecimal stockMinimo,
    String estado,
    CategoriaResponse categoria,
    MarcaResponse marca,
    UnidadMedidaResponse unidadBase,
    Instant fechaRegistro
) {
    public static ProductoResponse from(Producto producto) {
        return new ProductoResponse(
            producto.getId(),
            producto.getCodigoInterno(),
            producto.getCodigoBarras(),
            producto.getNombre(),
            producto.getDescripcion(),
            producto.getStockMinimo(),
            producto.getEstado().name(),
            CategoriaResponse.from(producto.getCategoria()),
            producto.getMarca() == null ? null : MarcaResponse.from(producto.getMarca()),
            UnidadMedidaResponse.from(producto.getUnidadBase()),
            producto.getFechaRegistro()
        );
    }
}
