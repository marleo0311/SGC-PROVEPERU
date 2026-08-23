package pe.com.proveperu.sgc.comprobante.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import pe.com.proveperu.sgc.comprobante.domain.model.Comprobante;
import pe.com.proveperu.sgc.comprobante.domain.model.EstadoComprobante;
import pe.com.proveperu.sgc.facturacionelectronica.api.dto.EnvioSunatResponse;
import pe.com.proveperu.sgc.venta.api.dto.VentaDetalleResponse;
import pe.com.proveperu.sgc.venta.api.dto.VentaResumenResponse;
import pe.com.proveperu.sgc.venta.domain.model.TipoComprobanteVenta;

public record ComprobanteResponse(
    Long id,
    Long idVenta,
    TipoComprobanteVenta tipo,
    String serie,
    String numero,
    String numeroCompleto,
    Instant fechaEmision,
    BigDecimal subtotal,
    BigDecimal igv,
    BigDecimal total,
    EstadoComprobante estado,
    Instant fechaAnulacion,
    String motivoAnulacion,
    Long idUsuarioAnulacion,
    String usuarioAnulacion,
    EnvioSunatResponse envioSunat,
    VentaResumenResponse venta,
    List<VentaDetalleResponse> items
) {
    public static ComprobanteResponse from(Comprobante comprobante) {
        return new ComprobanteResponse(
            comprobante.getId(),
            comprobante.getVenta().getId(),
            comprobante.getTipo(),
            comprobante.getSerie(),
            comprobante.getNumero(),
            comprobante.getNumeroCompleto(),
            comprobante.getFechaEmision(),
            comprobante.getSubtotal(),
            comprobante.getIgv(),
            comprobante.getTotal(),
            comprobante.getEstado(),
            comprobante.getFechaAnulacion(),
            comprobante.getMotivoAnulacion(),
            comprobante.getUsuarioAnulacion() == null
                ? null
                : comprobante.getUsuarioAnulacion().getId(),
            comprobante.getUsuarioAnulacion() == null
                ? null
                : comprobante.getUsuarioAnulacion().getUsuarioLogin(),
            comprobante.getEnvioSunat() == null
                ? null
                : EnvioSunatResponse.from(comprobante.getEnvioSunat()),
            VentaResumenResponse.from(comprobante.getVenta()),
            comprobante.getVenta().getDetalles().stream()
                .map(VentaDetalleResponse::from)
                .toList()
        );
    }
}
