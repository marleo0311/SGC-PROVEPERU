package pe.com.proveperu.sgc.facturacionelectronica.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import pe.com.proveperu.sgc.comprobante.domain.model.Comprobante;
import pe.com.proveperu.sgc.configuracion.domain.model.Empresa;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.ComunicacionBajaSunat;

class GeneradorComunicacionBajaUblServiceTests {
    @Test
    void generaVoidedDocumentsParaFactura() {
        Comprobante comprobante = new Comprobante(); comprobante.setSerie("F001"); comprobante.setNumero("00000012");
        ComunicacionBajaSunat baja = new ComunicacionBajaSunat(); baja.setComprobante(comprobante); baja.setFechaDocumento(LocalDate.of(2026, 8, 23)); baja.setFechaGeneracion(LocalDate.of(2026, 8, 24)); baja.setCorrelativo(7); baja.setMotivo("ERROR EN LA OPERACION");
        Empresa empresa = new Empresa(); empresa.setRuc("20612296911"); empresa.setRazonSocial("PROVEPERU S.R.L.");

        String xml = new String(new GeneradorComunicacionBajaUblService().generar(baja, empresa).xml(), StandardCharsets.UTF_8);

        assertThat(xml).contains("<VoidedDocuments").contains("<cbc:ID>RA-20260824-7</cbc:ID>").contains("<cbc:ReferenceDate>2026-08-23</cbc:ReferenceDate>").contains("<cbc:DocumentTypeCode>01</cbc:DocumentTypeCode>").contains("<sac:DocumentSerialID>F001</sac:DocumentSerialID>").contains("<sac:DocumentNumberID>00000012</sac:DocumentNumberID>").contains("<sac:VoidReasonDescription>ERROR EN LA OPERACION</sac:VoidReasonDescription>");
    }
}
