package pe.com.proveperu.sgc.impresion.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.com.proveperu.sgc.cliente.domain.model.TipoDocumentoCliente;
import pe.com.proveperu.sgc.comprobante.api.dto.ClienteComprobanteResponse;
import pe.com.proveperu.sgc.comprobante.api.dto.ComprobanteResponse;
import pe.com.proveperu.sgc.comprobante.api.dto.EmpresaComprobanteResponse;
import pe.com.proveperu.sgc.comprobante.api.dto.RepresentacionComprobanteResponse;
import pe.com.proveperu.sgc.comprobante.domain.model.EstadoComprobante;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.AmbienteSunat;
import pe.com.proveperu.sgc.venta.domain.model.TipoComprobanteVenta;

class QrComprobanteServiceTests {
    @Test
    void generaQrPngConCamposTributariosSunat() {
        var comprobante = new ComprobanteResponse(
            1L, 1L, TipoComprobanteVenta.BOLETA, AmbienteSunat.BETA,
            "B001", "00000049", "B001-00000049",
            Instant.parse("2026-08-22T15:00:00Z"), new BigDecimal("118.64"),
            new BigDecimal("21.36"), new BigDecimal("140.00"), EstadoComprobante.EMITIDO,
            null, null, null, null, null, null, List.of()
        );
        var representacion = new RepresentacionComprobanteResponse(
            "BOLETA DE VENTA ELECTRÓNICA", "PEN",
            new EmpresaComprobanteResponse("20612296911", "PROVEPERU S.R.L.", null, null, null, 1L, "Principal", null),
            new ClienteComprobanteResponse(1L, TipoDocumentoCliente.DNI, "12345678", "Marco Quiroz", null),
            comprobante, "Representación impresa"
        );

        var qr = new QrComprobanteService().generar(representacion);
        byte[] png = Base64.getDecoder().decode(qr.imagenPngBase64());

        assertThat(qr.contenido()).isEqualTo("20612296911|03|B001|00000049|21.36|140.00|2026-08-22|1|12345678||");
        assertThat(png).startsWith(new byte[] {(byte) 0x89, 'P', 'N', 'G'});
    }
}
