package pe.com.proveperu.sgc.facturacionelectronica.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

class CdrParserTests {

    private final CdrParser parser = new CdrParser();

    @Test
    void obtieneAceptacionYObservacionesDelCdr() throws Exception {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <ApplicationResponse xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2">
              <cbc:ResponseCode>0</cbc:ResponseCode>
              <cbc:Description>La Factura numero F001-1 ha sido aceptada</cbc:Description>
              <cbc:Note>Observación de prueba</cbc:Note>
            </ApplicationResponse>
            """;

        var result = parser.procesar(zip("R-20612296911-01-F001-1.xml", xml));

        assertThat(result.aceptado()).isTrue();
        assertThat(result.codigo()).isEqualTo("0");
        assertThat(result.observaciones()).containsExactly("Observación de prueba");
    }

    private byte[] zip(String name, String content) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry(name));
            zip.write(content.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return output.toByteArray();
    }
}
