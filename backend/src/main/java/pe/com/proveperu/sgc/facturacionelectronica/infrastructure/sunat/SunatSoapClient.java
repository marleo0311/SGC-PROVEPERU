package pe.com.proveperu.sgc.facturacionelectronica.infrastructure.sunat;

import java.io.ByteArrayInputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import pe.com.proveperu.sgc.facturacionelectronica.application.port.SunatGateway;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.config.SunatProperties;

@Component
public class SunatSoapClient implements SunatGateway {

    private final SunatProperties properties;
    private final HttpClient httpClient;

    public SunatSoapClient(SunatProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(properties.getConnectTimeout())
            .build();
    }

    @Override
    public byte[] enviarComprobante(String ruc, String nombreZip, byte[] contenidoZip) {
        validarConfiguracion();
        String envelope = envelope(
            properties.username(ruc),
            properties.getClaveSol(),
            nombreZip,
            Base64.getEncoder().encodeToString(contenidoZip)
        );
        HttpRequest request = HttpRequest.newBuilder(properties.endpoint())
            .timeout(properties.getReadTimeout())
            .header("Content-Type", "text/xml; charset=UTF-8")
            .header("SOAPAction", "")
            .POST(HttpRequest.BodyPublishers.ofString(envelope, StandardCharsets.UTF_8))
            .build();
        try {
            HttpResponse<byte[]> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofByteArray()
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IntegracionSunatException(
                    "SUNAT respondió con HTTP " + response.statusCode()
                );
            }
            return extraerCdr(response.body());
        } catch (IntegracionSunatException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IntegracionSunatException("El envío a SUNAT fue interrumpido", exception);
        } catch (Exception exception) {
            throw new IntegracionSunatException(
                "No se pudo establecer comunicación con SUNAT",
                exception
            );
        }
    }

    byte[] extraerCdr(byte[] soap) {
        try {
            Document document = parsear(soap);
            String fault = texto(document, "faultstring");
            if (fault != null) {
                String code = texto(document, "faultcode");
                throw new RechazoSunatException(
                    codigoFault(code),
                    "SUNAT rechazó la solicitud%s: %s".formatted(
                        code == null ? "" : " (" + code + ")",
                        fault
                    )
                );
            }
            String applicationResponse = texto(document, "applicationResponse");
            if (applicationResponse == null || applicationResponse.isBlank()) {
                throw new IntegracionSunatException("La respuesta SOAP de SUNAT no contiene un CDR");
            }
            return Base64.getMimeDecoder().decode(applicationResponse.strip());
        } catch (IntegracionSunatException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IntegracionSunatException("No se pudo interpretar la respuesta SOAP de SUNAT", exception);
        }
    }

    private void validarConfiguracion() {
        if (!properties.isEnabled()) {
            throw new IntegracionSunatException(
                "La integración está deshabilitada; configura SUNAT_ENABLED=true para realizar pruebas"
            );
        }
        if (properties.getAmbiente().name().equals("PRODUCCION")
            && !properties.isProductionEnabled()) {
            throw new IntegracionSunatException(
                "El envío a producción está bloqueado por SUNAT_PRODUCTION_ENABLED=false"
            );
        }
        if (properties.getUsuarioSol() == null || properties.getUsuarioSol().isBlank()
            || properties.getClaveSol() == null || properties.getClaveSol().isBlank()) {
            throw new IntegracionSunatException("Configura las credenciales SOL para el ambiente seleccionado");
        }
    }

    private String envelope(
        String username,
        String password,
        String fileName,
        String content
    ) {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                              xmlns:ser="http://service.sunat.gob.pe"
                              xmlns:wsse="http://schemas.xmlsoap.org/ws/2002/12/secext">
              <soapenv:Header>
                <wsse:Security>
                  <wsse:UsernameToken>
                    <wsse:Username>%s</wsse:Username>
                    <wsse:Password>%s</wsse:Password>
                  </wsse:UsernameToken>
                </wsse:Security>
              </soapenv:Header>
              <soapenv:Body>
                <ser:sendBill>
                  <fileName>%s</fileName>
                  <contentFile>%s</contentFile>
                </ser:sendBill>
              </soapenv:Body>
            </soapenv:Envelope>
            """.formatted(
                escape(username),
                escape(password),
                escape(fileName),
                content
            );
    }

    private Document parsear(byte[] xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
    }

    private String texto(Document document, String localName) {
        NodeList nodes = document.getElementsByTagNameNS("*", localName);
        return nodes.getLength() == 0 ? null : nodes.item(0).getTextContent();
    }

    private String codigoFault(String value) {
        if (value == null || value.isBlank()) {
            return "SOAP_FAULT";
        }
        int separator = value.lastIndexOf('.');
        return separator >= 0 && separator + 1 < value.length()
            ? value.substring(separator + 1)
            : value;
    }

    private String escape(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}
