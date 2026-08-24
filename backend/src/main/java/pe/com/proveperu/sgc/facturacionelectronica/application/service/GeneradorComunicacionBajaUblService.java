package pe.com.proveperu.sgc.facturacionelectronica.application.service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import pe.com.proveperu.sgc.configuracion.domain.model.Empresa;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.DocumentoUbl;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.ComunicacionBajaSunat;

@Service
public class GeneradorComunicacionBajaUblService {

    private static final String ROOT_NS = "urn:sunat:names:specification:ubl:peru:schema:xsd:VoidedDocuments-1";
    private static final String SAC_NS = "urn:sunat:names:specification:ubl:peru:schema:xsd:SunatAggregateComponents-1";
    private static final String CAC_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    private static final String CBC_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
    private static final String EXT_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";
    private static final String DS_NS = "http://www.w3.org/2000/09/xmldsig#";

    public DocumentoUbl generar(ComunicacionBajaSunat baja, Empresa empresa) {
        try {
            Document document = documento();
            Element root = document.createElementNS(ROOT_NS, "VoidedDocuments");
            root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns", ROOT_NS);
            root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:sac", SAC_NS);
            root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:cac", CAC_NS);
            root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:cbc", CBC_NS);
            root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:ext", EXT_NS);
            root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:ds", DS_NS);
            document.appendChild(root);

            Element extensions = agregar(document, root, EXT_NS, "ext:UBLExtensions");
            Element extension = agregar(document, extensions, EXT_NS, "ext:UBLExtension");
            agregar(document, extension, EXT_NS, "ext:ExtensionContent");
            texto(document, root, CBC_NS, "cbc:UBLVersionID", "2.0");
            texto(document, root, CBC_NS, "cbc:CustomizationID", "1.0");
            String id = "RA-" + baja.getFechaGeneracion().toString().replace("-", "") + "-" + baja.getCorrelativo();
            texto(document, root, CBC_NS, "cbc:ID", id);
            texto(document, root, CBC_NS, "cbc:ReferenceDate", baja.getFechaDocumento().toString());
            texto(document, root, CBC_NS, "cbc:IssueDate", baja.getFechaGeneracion().toString());

            Element signature = agregar(document, root, CAC_NS, "cac:Signature");
            texto(document, signature, CBC_NS, "cbc:ID", "SIGN-" + id);
            Element signatory = agregar(document, signature, CAC_NS, "cac:SignatoryParty");
            Element identification = agregar(document, signatory, CAC_NS, "cac:PartyIdentification");
            texto(document, identification, CBC_NS, "cbc:ID", empresa.getRuc());
            Element partyName = agregar(document, signatory, CAC_NS, "cac:PartyName");
            texto(document, partyName, CBC_NS, "cbc:Name", empresa.getRazonSocial());
            Element attachment = agregar(document, signature, CAC_NS, "cac:DigitalSignatureAttachment");
            Element reference = agregar(document, attachment, CAC_NS, "cac:ExternalReference");
            texto(document, reference, CBC_NS, "cbc:URI", "#signatureKG");

            Element supplier = agregar(document, root, CAC_NS, "cac:AccountingSupplierParty");
            texto(document, supplier, CBC_NS, "cbc:CustomerAssignedAccountID", empresa.getRuc());
            texto(document, supplier, CBC_NS, "cbc:AdditionalAccountID", "6");
            Element party = agregar(document, supplier, CAC_NS, "cac:Party");
            Element legal = agregar(document, party, CAC_NS, "cac:PartyLegalEntity");
            texto(document, legal, CBC_NS, "cbc:RegistrationName", empresa.getRazonSocial());

            Element line = agregar(document, root, SAC_NS, "sac:VoidedDocumentsLine");
            texto(document, line, CBC_NS, "cbc:LineID", "1");
            texto(document, line, CBC_NS, "cbc:DocumentTypeCode", "01");
            texto(document, line, SAC_NS, "sac:DocumentSerialID", baja.getComprobante().getSerie());
            texto(document, line, SAC_NS, "sac:DocumentNumberID", baja.getComprobante().getNumero());
            texto(document, line, SAC_NS, "sac:VoidReasonDescription", baja.getMotivo());

            return new DocumentoUbl(empresa.getRuc() + "-" + id, serializar(document));
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo generar la comunicación de baja UBL", exception);
        }
    }

    private Document documento() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().newDocument();
    }

    private Element agregar(Document document, Element parent, String namespace, String name) {
        Element element = document.createElementNS(namespace, name);
        parent.appendChild(element);
        return element;
    }

    private Element texto(Document document, Element parent, String namespace, String name, String value) {
        Element element = agregar(document, parent, namespace, name);
        element.setTextContent(value);
        return element;
    }

    private byte[] serializar(Document document) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        var transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name());
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(document), new StreamResult(output));
        return output.toByteArray();
    }
}
