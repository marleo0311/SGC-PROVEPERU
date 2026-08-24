package pe.com.proveperu.sgc.facturacionelectronica.application.service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import pe.com.proveperu.sgc.cliente.domain.model.Cliente;
import pe.com.proveperu.sgc.cliente.domain.model.TipoDocumentoCliente;
import pe.com.proveperu.sgc.comprobante.domain.model.Comprobante;
import pe.com.proveperu.sgc.configuracion.domain.model.Empresa;
import pe.com.proveperu.sgc.facturacionelectronica.application.dto.DocumentoUbl;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.NotaElectronica;
import pe.com.proveperu.sgc.facturacionelectronica.domain.model.TipoNotaElectronica;

@Service
public class GeneradorNotaElectronicaUblService {

    private static final String CREDIT_NS = "urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2";
    private static final String DEBIT_NS = "urn:oasis:names:specification:ubl:schema:xsd:DebitNote-2";
    private static final String CAC_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    private static final String CBC_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
    private static final String EXT_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";
    private static final String DS_NS = "http://www.w3.org/2000/09/xmldsig#";
    private static final ZoneId LIMA = ZoneId.of("America/Lima");

    public DocumentoUbl generar(NotaElectronica nota, Empresa empresa) {
        try {
            boolean credito = nota.getTipo() == TipoNotaElectronica.CREDITO;
            String namespace = credito ? CREDIT_NS : DEBIT_NS;
            String rootName = credito ? "CreditNote" : "DebitNote";
            String lineName = credito ? "cac:CreditNoteLine" : "cac:DebitNoteLine";
            String quantityName = credito ? "cbc:CreditedQuantity" : "cbc:DebitedQuantity";
            String codigoTipo = credito ? "07" : "08";
            Comprobante origen = nota.getComprobanteOrigen();
            Document document = documento();
            Element root = document.createElementNS(namespace, rootName);
            root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns", namespace);
            root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:cac", CAC_NS);
            root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:cbc", CBC_NS);
            root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:ext", EXT_NS);
            root.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:ds", DS_NS);
            document.appendChild(root);

            extensionFirma(document, root);
            texto(document, root, CBC_NS, "cbc:UBLVersionID", "2.1");
            texto(document, root, CBC_NS, "cbc:CustomizationID", "2.0");
            texto(document, root, CBC_NS, "cbc:ID", nota.getNumeroCompleto());
            var fecha = nota.getFechaEmision().atZone(LIMA);
            texto(document, root, CBC_NS, "cbc:IssueDate", fecha.toLocalDate().toString());
            texto(document, root, CBC_NS, "cbc:IssueTime", fecha.format(DateTimeFormatter.ofPattern("HH:mm:ssXXX")));
            texto(document, root, CBC_NS, "cbc:DocumentCurrencyCode", "PEN");

            Element discrepancy = agregar(document, root, CAC_NS, "cac:DiscrepancyResponse");
            texto(document, discrepancy, CBC_NS, "cbc:ReferenceID", origen.getNumeroCompleto());
            Element reasonCode = texto(document, discrepancy, CBC_NS, "cbc:ResponseCode", nota.getCodigoMotivo());
            reasonCode.setAttribute("listAgencyName", "PE:SUNAT");
            reasonCode.setAttribute("listName", credito ? "Tipo de nota de credito" : "Tipo de nota de debito");
            reasonCode.setAttribute("listURI", credito
                ? "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo09"
                : "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo10");
            texto(document, discrepancy, CBC_NS, "cbc:Description", nota.getDescripcionMotivo());

            Element billing = agregar(document, root, CAC_NS, "cac:BillingReference");
            Element reference = agregar(document, billing, CAC_NS, "cac:InvoiceDocumentReference");
            texto(document, reference, CBC_NS, "cbc:ID", origen.getNumeroCompleto());
            texto(document, reference, CBC_NS, "cbc:DocumentTypeCode", codigoOrigen(origen));

            firma(document, root, nota, empresa);
            proveedor(document, root, empresa);
            cliente(document, root, origen.getVenta().getCliente());
            totales(document, root, nota);

            Element line = agregar(document, root, CAC_NS, lineName);
            texto(document, line, CBC_NS, "cbc:ID", "1");
            Element quantity = texto(document, line, CBC_NS, quantityName, "1");
            quantity.setAttribute("unitCode", "NIU");
            importe(document, line, "cbc:LineExtensionAmount", nota.getSubtotal());
            tributos(document, line, nota.getSubtotal(), nota.getIgv());
            Element item = agregar(document, line, CAC_NS, "cac:Item");
            texto(document, item, CBC_NS, "cbc:Description", nota.getDescripcionMotivo());
            Element price = agregar(document, line, CAC_NS, "cac:Price");
            importe(document, price, "cbc:PriceAmount", nota.getSubtotal());

            return new DocumentoUbl(
                empresa.getRuc() + "-" + codigoTipo + "-" + nota.getNumeroCompleto(),
                serializar(document)
            );
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo generar la nota electrónica UBL 2.1", exception);
        }
    }

    private void totales(Document document, Element root, NotaElectronica nota) {
        tributos(document, root, nota.getSubtotal(), nota.getIgv());
        Element monetary = agregar(document, root, CAC_NS, "cac:RequestedMonetaryTotal");
        importe(document, monetary, "cbc:LineExtensionAmount", nota.getSubtotal());
        importe(document, monetary, "cbc:TaxInclusiveAmount", nota.getTotal());
        importe(document, monetary, "cbc:PayableAmount", nota.getTotal());
    }

    private void tributos(Document document, Element parent, BigDecimal base, BigDecimal igv) {
        Element taxTotal = agregar(document, parent, CAC_NS, "cac:TaxTotal");
        importe(document, taxTotal, "cbc:TaxAmount", igv);
        Element subtotal = agregar(document, taxTotal, CAC_NS, "cac:TaxSubtotal");
        importe(document, subtotal, "cbc:TaxableAmount", base);
        importe(document, subtotal, "cbc:TaxAmount", igv);
        Element category = agregar(document, subtotal, CAC_NS, "cac:TaxCategory");
        texto(document, category, CBC_NS, "cbc:Percent", "18.00");
        texto(document, category, CBC_NS, "cbc:TaxExemptionReasonCode", "10");
        Element scheme = agregar(document, category, CAC_NS, "cac:TaxScheme");
        texto(document, scheme, CBC_NS, "cbc:ID", "1000");
        texto(document, scheme, CBC_NS, "cbc:Name", "IGV");
        texto(document, scheme, CBC_NS, "cbc:TaxTypeCode", "VAT");
    }

    private void proveedor(Document document, Element root, Empresa empresa) {
        Element supplier = agregar(document, root, CAC_NS, "cac:AccountingSupplierParty");
        Element party = agregar(document, supplier, CAC_NS, "cac:Party");
        Element identification = agregar(document, party, CAC_NS, "cac:PartyIdentification");
        documentoIdentidad(document, identification, empresa.getRuc(), "6");
        Element legal = agregar(document, party, CAC_NS, "cac:PartyLegalEntity");
        texto(document, legal, CBC_NS, "cbc:RegistrationName", empresa.getRazonSocial());
    }

    private void cliente(Document document, Element root, Cliente cliente) {
        Element customer = agregar(document, root, CAC_NS, "cac:AccountingCustomerParty");
        Element party = agregar(document, customer, CAC_NS, "cac:Party");
        Element identification = agregar(document, party, CAC_NS, "cac:PartyIdentification");
        String numero = cliente == null ? "-" : cliente.getNumeroDocumento();
        String tipo = cliente == null ? "0" : cliente.getTipoDocumento() == TipoDocumentoCliente.RUC ? "6" : "1";
        documentoIdentidad(document, identification, numero, tipo);
        Element legal = agregar(document, party, CAC_NS, "cac:PartyLegalEntity");
        String nombre = cliente == null ? "CLIENTE VARIOS" : nombreCliente(cliente);
        texto(document, legal, CBC_NS, "cbc:RegistrationName", nombre);
    }

    private void documentoIdentidad(Document document, Element parent, String numero, String tipo) {
        Element id = texto(document, parent, CBC_NS, "cbc:ID", numero);
        id.setAttribute("schemeID", tipo);
        id.setAttribute("schemeName", "Documento de Identidad");
        id.setAttribute("schemeAgencyName", "PE:SUNAT");
        id.setAttribute("schemeURI", "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo06");
    }

    private void firma(Document document, Element root, NotaElectronica nota, Empresa empresa) {
        Element signature = agregar(document, root, CAC_NS, "cac:Signature");
        texto(document, signature, CBC_NS, "cbc:ID", "SIGN-" + nota.getNumeroCompleto());
        Element signatory = agregar(document, signature, CAC_NS, "cac:SignatoryParty");
        Element identification = agregar(document, signatory, CAC_NS, "cac:PartyIdentification");
        texto(document, identification, CBC_NS, "cbc:ID", empresa.getRuc());
        Element name = agregar(document, signatory, CAC_NS, "cac:PartyName");
        texto(document, name, CBC_NS, "cbc:Name", empresa.getRazonSocial());
        Element attachment = agregar(document, signature, CAC_NS, "cac:DigitalSignatureAttachment");
        Element external = agregar(document, attachment, CAC_NS, "cac:ExternalReference");
        texto(document, external, CBC_NS, "cbc:URI", "#signatureKG");
    }

    private void extensionFirma(Document document, Element root) {
        Element extensions = agregar(document, root, EXT_NS, "ext:UBLExtensions");
        Element extension = agregar(document, extensions, EXT_NS, "ext:UBLExtension");
        agregar(document, extension, EXT_NS, "ext:ExtensionContent");
    }

    private String codigoOrigen(Comprobante comprobante) {
        return switch (comprobante.getTipo()) { case FACTURA -> "01"; case BOLETA -> "03"; case NOTA_VENTA -> "00"; };
    }

    private String nombreCliente(Cliente cliente) {
        if (cliente.getRazonSocial() != null && !cliente.getRazonSocial().isBlank()) return cliente.getRazonSocial();
        return ((cliente.getNombres() == null ? "" : cliente.getNombres()) + " " + (cliente.getApellidos() == null ? "" : cliente.getApellidos())).strip();
    }

    private Document documento() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true); factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().newDocument();
    }

    private Element importe(Document document, Element parent, String name, BigDecimal value) {
        Element amount = texto(document, parent, CBC_NS, name, value.setScale(2, RoundingMode.HALF_UP).toPlainString());
        amount.setAttribute("currencyID", "PEN"); return amount;
    }
    private Element agregar(Document document, Element parent, String namespace, String name) { Element element = document.createElementNS(namespace, name); parent.appendChild(element); return element; }
    private Element texto(Document document, Element parent, String namespace, String name, String value) { Element element = agregar(document, parent, namespace, name); element.setTextContent(value); return element; }
    private byte[] serializar(Document document) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance(); factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, ""); factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        var transformer = factory.newTransformer(); transformer.setOutputProperty(OutputKeys.ENCODING, StandardCharsets.UTF_8.name()); transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no"); transformer.setOutputProperty(OutputKeys.INDENT, "no");
        ByteArrayOutputStream output = new ByteArrayOutputStream(); transformer.transform(new DOMSource(document), new StreamResult(output)); return output.toByteArray();
    }
}
