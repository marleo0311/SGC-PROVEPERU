package pe.com.proveperu.sgc.cliente.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import pe.com.proveperu.sgc.cliente.domain.model.ClientePrecioEspecial;

public record ClientePrecioEspecialResponse(
    Long id,
    Long idCliente,
    Long idProducto,
    String codigoProducto,
    String nombreProducto,
    BigDecimal precio,
    LocalDate vigenteDesde,
    LocalDate vigenteHasta,
    String estado,
    Instant fechaRegistro
) {
    public static ClientePrecioEspecialResponse from(ClientePrecioEspecial precio) {
        return new ClientePrecioEspecialResponse(
            precio.getId(),
            precio.getCliente().getId(),
            precio.getProducto().getId(),
            precio.getProducto().getCodigoInterno(),
            precio.getProducto().getNombre(),
            precio.getPrecio(),
            precio.getVigenteDesde(),
            precio.getVigenteHasta(),
            precio.getEstado().name(),
            precio.getFechaRegistro()
        );
    }
}
