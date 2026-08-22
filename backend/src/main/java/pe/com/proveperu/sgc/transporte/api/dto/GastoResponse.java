package pe.com.proveperu.sgc.transporte.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import pe.com.proveperu.sgc.transporte.domain.model.Gasto;
import pe.com.proveperu.sgc.transporte.domain.model.TipoGasto;

public record GastoResponse(
    Long id,
    Long idCompra,
    Long idTransportista,
    String transportista,
    Long idUsuario,
    String usuarioLogin,
    TipoGasto tipoGasto,
    String descripcion,
    BigDecimal importe,
    LocalDate fecha,
    String numeroComprobante,
    Instant fechaRegistro
) {
    public static GastoResponse from(Gasto gasto) {
        Long idTransportista = gasto.getTransportista() == null
            ? null
            : gasto.getTransportista().getId();
        String nombreTransportista = gasto.getTransportista() == null
            ? null
            : gasto.getTransportista().getNombreRazonSocial();
        return new GastoResponse(
            gasto.getId(),
            gasto.getIdCompra(),
            idTransportista,
            nombreTransportista,
            gasto.getUsuario().getId(),
            gasto.getUsuario().getUsuarioLogin(),
            gasto.getTipoGasto(),
            gasto.getDescripcion(),
            gasto.getImporte(),
            gasto.getFecha(),
            gasto.getNumeroComprobante(),
            gasto.getFechaRegistro()
        );
    }
}
