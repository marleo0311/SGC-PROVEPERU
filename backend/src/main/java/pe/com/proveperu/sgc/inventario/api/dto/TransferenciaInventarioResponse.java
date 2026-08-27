package pe.com.proveperu.sgc.inventario.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import pe.com.proveperu.sgc.inventario.domain.model.TransferenciaInventario;

public record TransferenciaInventarioResponse(
    Long id,
    Long idSedeOrigen,
    String sedeOrigen,
    Long idSedeDestino,
    String sedeDestino,
    Long idProducto,
    String codigoProducto,
    String producto,
    BigDecimal cantidad,
    String unidadMedida,
    BigDecimal cantidadBase,
    String unidadBase,
    String motivo,
    String usuario,
    Instant fechaHora,
    MovimientoInventarioResponse movimientoSalida,
    MovimientoInventarioResponse movimientoEntrada,
    StockInventarioResponse stockOrigen,
    StockInventarioResponse stockDestino
) {
    public static TransferenciaInventarioResponse from(
        TransferenciaInventario transferencia,
        MovimientoInventarioResponse movimientoSalida,
        MovimientoInventarioResponse movimientoEntrada,
        StockInventarioResponse stockOrigen,
        StockInventarioResponse stockDestino
    ) {
        return new TransferenciaInventarioResponse(
            transferencia.getId(),
            transferencia.getSedeOrigen().getId(),
            transferencia.getSedeOrigen().getNombre(),
            transferencia.getSedeDestino().getId(),
            transferencia.getSedeDestino().getNombre(),
            transferencia.getProducto().getId(),
            transferencia.getProducto().getCodigoInterno(),
            transferencia.getProducto().getNombre(),
            transferencia.getCantidad(),
            transferencia.getUnidadMedida().getCodigo(),
            transferencia.getCantidadBase(),
            transferencia.getProducto().getUnidadBase().getCodigo(),
            transferencia.getMotivo(),
            transferencia.getUsuario().getUsuarioLogin(),
            transferencia.getFechaHora(),
            movimientoSalida,
            movimientoEntrada,
            stockOrigen,
            stockDestino
        );
    }
}
