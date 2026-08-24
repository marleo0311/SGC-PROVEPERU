package pe.com.proveperu.sgc.facturacionelectronica.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import pe.com.proveperu.sgc.cliente.domain.model.Cliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoDocumentoCliente;
import pe.com.proveperu.sgc.comprobante.domain.model.Comprobante;
import pe.com.proveperu.sgc.comprobante.domain.model.EstadoComprobante;
import pe.com.proveperu.sgc.configuracion.domain.model.Empresa;
import pe.com.proveperu.sgc.inventario.domain.model.Sede;
import pe.com.proveperu.sgc.shared.application.exception.ReglaNegocioException;
import pe.com.proveperu.sgc.venta.domain.model.TipoComprobanteVenta;
import pe.com.proveperu.sgc.venta.domain.model.Venta;

class GeneradorResumenDiarioUblServiceTests {

    private final GeneradorResumenDiarioUblService service =
        new GeneradorResumenDiarioUblService();

    @Test
    void generaSummaryDocumentsConBoletasEImportesIgvIncluido() {
        String xml = new String(
            service.generar(
                List.of(boleta("00000045")),
                empresa(),
                LocalDate.of(2026, 8, 22),
                LocalDate.of(2026, 8, 23),
                1
            ).xml(),
            StandardCharsets.UTF_8
        );

        assertThat(xml)
            .contains("<SummaryDocuments")
            .contains("xmlns:sac=\"urn:sunat:names:specification:ubl:peru:schema:xsd:SunatAggregateComponents-1\"")
            .contains("<cbc:UBLVersionID>2.0</cbc:UBLVersionID>")
            .contains("<cbc:CustomizationID>1.1</cbc:CustomizationID>")
            .contains("<cbc:ID>RC-20260822-1</cbc:ID>")
            .contains("<cbc:ReferenceDate>2026-08-22</cbc:ReferenceDate>")
            .contains("<cbc:IssueDate>2026-08-23</cbc:IssueDate>")
            .contains("<cbc:DocumentTypeCode>03</cbc:DocumentTypeCode>")
            .contains("<cbc:ID>B001-00000045</cbc:ID>")
            .contains("<cbc:ConditionCode>1</cbc:ConditionCode>")
            .contains("<sac:TotalAmount currencyID=\"PEN\">140.00</sac:TotalAmount>")
            .contains("<cbc:PaidAmount currencyID=\"PEN\">118.64</cbc:PaidAmount>")
            .contains("<cbc:InstructionID>01</cbc:InstructionID>")
            .contains("<cbc:TaxAmount currencyID=\"PEN\">21.36</cbc:TaxAmount>");
    }

    @Test
    void informaClienteOcasionalConGuionesSegunFormatoSunat() {
        Comprobante comprobante = boleta("00000046");
        comprobante.getVenta().setCliente(null);

        String xml = new String(service.generar(
            List.of(comprobante),
            empresa(),
            LocalDate.of(2026, 8, 22),
            LocalDate.of(2026, 8, 23),
            2
        ).xml(), StandardCharsets.UTF_8);

        assertThat(xml)
            .contains("<cbc:CustomerAssignedAccountID>-</cbc:CustomerAssignedAccountID>")
            .contains("<cbc:AdditionalAccountID>-</cbc:AdditionalAccountID>");
    }

    @Test
    void rechazaDocumentosQueNoSeanBoletas() {
        Comprobante comprobante = boleta("00000045");
        comprobante.setTipo(TipoComprobanteVenta.FACTURA);

        assertThatThrownBy(() -> service.generar(
            List.of(comprobante),
            empresa(),
            LocalDate.of(2026, 8, 22),
            LocalDate.of(2026, 8, 23),
            1
        ))
            .isInstanceOf(ReglaNegocioException.class)
            .hasMessageContaining("solo admite boletas");
    }

    @Test
    void usaCondicionTresParaAnularUnaBoletaEnElResumen() {
        Comprobante comprobante = boleta("00000047");
        comprobante.setEstado(EstadoComprobante.BAJA_PENDIENTE);

        String xml = new String(service.generar(
            List.of(comprobante), empresa(), LocalDate.of(2026, 8, 22),
            LocalDate.of(2026, 8, 23), 3
        ).xml(), StandardCharsets.UTF_8);

        assertThat(xml).contains("<cbc:ConditionCode>3</cbc:ConditionCode>");
    }

    private Comprobante boleta(String numero) {
        Cliente cliente = new Cliente();
        cliente.setTipoDocumento(TipoDocumentoCliente.DNI);
        cliente.setNumeroDocumento("12345678");
        Sede sede = new Sede();
        sede.setIdEmpresa(1L);
        Venta venta = new Venta();
        venta.setCliente(cliente);
        venta.setSede(sede);
        Comprobante comprobante = new Comprobante();
        comprobante.setTipo(TipoComprobanteVenta.BOLETA);
        comprobante.setSerie("B001");
        comprobante.setNumero(numero);
        comprobante.setFechaEmision(Instant.parse("2026-08-22T20:30:00Z"));
        comprobante.setSubtotal(new BigDecimal("118.64"));
        comprobante.setIgv(new BigDecimal("21.36"));
        comprobante.setTotal(new BigDecimal("140.00"));
        comprobante.setVenta(venta);
        return comprobante;
    }

    private Empresa empresa() {
        Empresa empresa = new Empresa();
        empresa.setRuc("20612296911");
        empresa.setRazonSocial("INVERSIONES PROVEPERU S.R.L.");
        return empresa;
    }
}
