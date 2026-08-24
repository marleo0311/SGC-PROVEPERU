package pe.com.proveperu.sgc.impresion.application.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import pe.com.proveperu.sgc.cliente.domain.model.TipoDocumentoCliente;
import pe.com.proveperu.sgc.comprobante.api.dto.RepresentacionComprobanteResponse;
import pe.com.proveperu.sgc.venta.domain.model.TipoComprobanteVenta;

@Service
public class QrComprobanteService {

    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Lima");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZONA_NEGOCIO);

    public ResultadoQr generar(RepresentacionComprobanteResponse representacion) {
        var comprobante = representacion.comprobante();
        var cliente = representacion.cliente();
        String valorResumen = comprobante.envioSunat() == null ? "" : valorSeguro(comprobante.envioSunat().hashXml());
        String contenido = String.join(
            "|",
            valorSeguro(representacion.emisor().ruc()),
            codigoComprobante(comprobante.tipo()),
            valorSeguro(comprobante.serie()),
            valorSeguro(comprobante.numero()),
            comprobante.igv().setScale(2, RoundingMode.HALF_UP).toPlainString(),
            comprobante.total().setScale(2, RoundingMode.HALF_UP).toPlainString(),
            FECHA.format(comprobante.fechaEmision()),
            codigoDocumento(cliente.tipoDocumento()),
            valorSeguro(cliente.numeroDocumento()),
            valorResumen
        ) + "|";
        return new ResultadoQr(contenido, imagenBase64(contenido));
    }

    private String imagenBase64(String contenido) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q);
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix matrix = new QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, 320, 320, hints);
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException | WriterException exception) {
            throw new IllegalStateException("No se pudo generar el código QR del comprobante", exception);
        }
    }

    private String codigoComprobante(TipoComprobanteVenta tipo) {
        return switch (tipo) {
            case FACTURA -> "01";
            case BOLETA -> "03";
            case NOTA_VENTA -> "00";
        };
    }

    private String codigoDocumento(TipoDocumentoCliente tipo) {
        if (tipo == null) return "";
        return tipo == TipoDocumentoCliente.RUC ? "6" : "1";
    }

    private String valorSeguro(String value) { return value == null ? "" : value.strip(); }

    public record ResultadoQr(String contenido, String imagenPngBase64) { }
}
