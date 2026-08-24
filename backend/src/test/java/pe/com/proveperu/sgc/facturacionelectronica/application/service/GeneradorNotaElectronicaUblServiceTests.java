package pe.com.proveperu.sgc.facturacionelectronica.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import pe.com.proveperu.sgc.cliente.domain.model.Cliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoDocumentoCliente;
import pe.com.proveperu.sgc.comprobante.domain.model.Comprobante;
import pe.com.proveperu.sgc.configuracion.domain.model.Empresa;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.NotaElectronica;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.TipoNotaElectronica;
import pe.com.proveperu.sgc.venta.domain.model.TipoComprobanteVenta;
import pe.com.proveperu.sgc.venta.domain.model.Venta;

class GeneradorNotaElectronicaUblServiceTests {
    private final GeneradorNotaElectronicaUblService service = new GeneradorNotaElectronicaUblService();

    @Test
    void generaNotaCreditoUbl21VinculadaALaBoleta() {
        Comprobante origen = new Comprobante(); origen.setTipo(TipoComprobanteVenta.BOLETA); origen.setSerie("B001"); origen.setNumero("00000049");
        Cliente cliente = new Cliente(); cliente.setTipoDocumento(TipoDocumentoCliente.DNI); cliente.setNumeroDocumento("12345678"); cliente.setNombres("Marco"); cliente.setApellidos("Quiroz");
        Venta venta = new Venta(); venta.setCliente(cliente); origen.setVenta(venta);
        NotaElectronica nota = new NotaElectronica(); nota.setComprobanteOrigen(origen); nota.setTipo(TipoNotaElectronica.CREDITO); nota.setSerie("BC01"); nota.setNumero("00000001"); nota.setCodigoMotivo("01"); nota.setDescripcionMotivo("ANULACION DE LA OPERACION"); nota.setFechaEmision(Instant.parse("2026-08-24T15:00:00Z")); nota.setSubtotal(new BigDecimal("118.64")); nota.setIgv(new BigDecimal("21.36")); nota.setTotal(new BigDecimal("140.00"));
        Empresa empresa = new Empresa(); empresa.setRuc("20612296911"); empresa.setRazonSocial("PROVEPERU S.R.L.");

        String xml = new String(service.generar(nota, empresa).xml(), StandardCharsets.UTF_8);

        assertThat(xml).contains("<CreditNote").contains("<cbc:UBLVersionID>2.1</cbc:UBLVersionID>").contains("<cbc:ID>BC01-00000001</cbc:ID>").contains("<cbc:ReferenceID>B001-00000049</cbc:ReferenceID>").contains("<cbc:ResponseCode").contains(">01</cbc:ResponseCode>").contains("<cbc:PayableAmount currencyID=\"PEN\">140.00</cbc:PayableAmount>");
    }
}
