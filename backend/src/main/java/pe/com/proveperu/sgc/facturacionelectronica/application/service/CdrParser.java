package pe.com.proveperu.sgc.facturacionelectronica.application.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.ResultadoCdr;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.sunat.IntegracionSunatException;

@Component
public class CdrParser {

    private static final int MAX_CDR_BYTES = 5 * 1024 * 1024;

    public ResultadoCdr procesar(byte[] cdrZip) {
        try {
            byte[] xml = extraerXml(cdrZip);
            Document document = parsear(xml);
            String codigo = requerido(document, "ResponseCode");
            String descripcion = requerido(document, "Description");
            List<String> observaciones = textos(document, "Note");
            return new ResultadoCdr(codigo, descripcion, observaciones);
        } catch (IntegracionSunatException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IntegracionSunatException("El CDR recibido de SUNAT no tiene un formato válido", exception);
        }
    }

    private byte[] extraerXml(byte[] zipBytes) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory() || !entry.getName().toLowerCase().endsWith(".xml")) {
                    continue;
                }
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int total = 0;
                int read;
                while ((read = zip.read(buffer)) != -1) {
                    total += read;
                    if (total > MAX_CDR_BYTES) {
                        throw new IntegracionSunatException("El CDR supera el tamaño máximo permitido");
                    }
                    output.write(buffer, 0, read);
                }
                return output.toByteArray();
            }
        }
        throw new IntegracionSunatException("El ZIP de respuesta no contiene el XML del CDR");
    }

    private Document parsear(byte[] xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
    }

    private String requerido(Document document, String localName) {
        NodeList nodes = document.getElementsByTagNameNS("*", localName);
        if (nodes.getLength() == 0 || nodes.item(0).getTextContent().isBlank()) {
            throw new IntegracionSunatException("El CDR no contiene " + localName);
        }
        return nodes.item(0).getTextContent().strip();
    }

    private List<String> textos(Document document, String localName) {
        NodeList nodes = document.getElementsByTagNameNS("*", localName);
        List<String> values = new ArrayList<>();
        for (int index = 0; index < nodes.getLength(); index++) {
            String value = nodes.item(index).getTextContent();
            if (value != null && !value.isBlank()) {
                values.add(value.strip());
            }
        }
        return List.copyOf(values);
    }
}
