package pe.com.proveperu.sgc.facturacionelectronica.application.service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
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
import pe.com.proveperu.sgc.shared.application.exception.ReglaNegocioException;
import pe.com.proveperu.sgc.venta.domain.model.TipoComprobanteVenta;

@Service
public class GeneradorResumenDiarioUblService {

    static final String SUMMARY_NS =
        "urn:sunat:names:specification:ubl:peru:schema:xsd:SummaryDocuments-1";
    static final String SAC_NS =
        "urn:sunat:names:specification:ubl:peru:schema:xsd:SunatAggregateComponents-1";
    static final String CAC_NS =
        "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    static final String CBC_NS =
        "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
    static final String EXT_NS =
        "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";
    static final String DS_NS = "http://www.w3.org/2000/09/xmldsig#";
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final BigDecimal LIMITE_CLIENTE_OBLIGATORIO = new BigDecimal("700.00");

    public DocumentoUbl generar(
        List<Comprobante> comprobantes,
        Empresa empresa,
        LocalDate fechaDocumentos,
        LocalDate fechaGeneracion,
        int correlativo
    ) {
        validar(comprobantes, empresa, fechaDocumentos, correlativo);
        try {
            String id = "RC-%s-%d".formatted(
                fechaDocumentos.toString().replace("-", ""),
                correlativo
            );
            Document document = documento();
            Element summary = document.createElementNS(SUMMARY_NS, "SummaryDocuments");
            summary.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns", SUMMARY_NS);
            summary.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:sac", SAC_NS);
            summary.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:cac", CAC_NS);
            summary.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:cbc", CBC_NS);
            summary.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:ext", EXT_NS);
            summary.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:ds", DS_NS);
            document.appendChild(summary);

            extensionFirma(document, summary);
            texto(document, summary, CBC_NS, "cbc:UBLVersionID", "2.0");
            texto(document, summary, CBC_NS, "cbc:CustomizationID", "1.1");
            texto(document, summary, CBC_NS, "cbc:ID", id);
            texto(document, summary, CBC_NS, "cbc:ReferenceDate", fechaDocumentos.toString());
            texto(document, summary, CBC_NS, "cbc:IssueDate", fechaGeneracion.toString());
            firmaDeclarada(document, summary, id, empresa);
            proveedor(document, summary, empresa);

            for (int index = 0; index < comprobantes.size(); index++) {
                linea(document, summary, index + 1, comprobantes.get(index));
            }
            return new DocumentoUbl(
                "%s-%s".formatted(empresa.getRuc(), id),
                serializar(document)
            );
        } catch (ReglaNegocioException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo generar el Resumen Diario UBL 2.0", exception);
        }
    }

    private void validar(
        List<Comprobante> comprobantes,
        Empresa empresa,
        LocalDate fechaDocumentos,
        int correlativo
    ) {
        if (comprobantes == null || comprobantes.isEmpty()) {
            throw new ReglaNegocioException("El resumen diario debe contener al menos una boleta");
        }
        if (comprobantes.size() > 500) {
            throw new ReglaNegocioException("Cada resumen diario admite como máximo 500 boletas");
        }
        if (empresa == null || empresa.getRuc() == null || !empresa.getRuc().matches("\\d{11}")) {
            throw new ReglaNegocioException("Configura un RUC válido para la empresa emisora");
        }
        if (empresa.getRazonSocial() == null || empresa.getRazonSocial().isBlank()) {
            throw new ReglaNegocioException("Configura la razón social de la empresa emisora");
        }
        if (correlativo <= 0) {
            throw new ReglaNegocioException("El correlativo del resumen diario no es válido");
        }
        for (Comprobante comprobante : comprobantes) {
            LocalDate fecha = comprobante.getFechaEmision().atZone(LIMA).toLocalDate();
            if (comprobante.getTipo() != TipoComprobanteVenta.BOLETA) {
                throw new ReglaNegocioException("El resumen diario solo admite boletas");
            }
            if (!fecha.equals(fechaDocumentos)) {
                throw new ReglaNegocioException("Todas las boletas deben pertenecer a la fecha resumida");
            }
            if (comprobante.getIgv() == null || comprobante.getIgv().signum() <= 0) {
                throw new ReglaNegocioException(
                    "La primera versión del resumen admite únicamente operaciones gravadas con IGV"
                );
            }
            Cliente cliente = comprobante.getVenta().getCliente();
            if (comprobante.getTotal().compareTo(LIMITE_CLIENTE_OBLIGATORIO) > 0
                && (cliente == null || cliente.getNumeroDocumento() == null
                    || cliente.getNumeroDocumento().isBlank())) {
                throw new ReglaNegocioException(
                    "La boleta " + comprobante.getNumeroCompleto()
                        + " supera S/ 700.00 y requiere documento del cliente"
                );
            }
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

    private void extensionFirma(Document document, Element root) {
        Element extensions = agregar(document, root, EXT_NS, "ext:UBLExtensions");
        Element extension = agregar(document, extensions, EXT_NS, "ext:UBLExtension");
        agregar(document, extension, EXT_NS, "ext:ExtensionContent");
    }

    private void firmaDeclarada(Document document, Element root, String id, Empresa empresa) {
        Element signature = agregar(document, root, CAC_NS, "cac:Signature");
        texto(document, signature, CBC_NS, "cbc:ID", "SIGN-" + id);
        Element signatory = agregar(document, signature, CAC_NS, "cac:SignatoryParty");
        Element identification = agregar(document, signatory, CAC_NS, "cac:PartyIdentification");
        texto(document, identification, CBC_NS, "cbc:ID", empresa.getRuc());
        Element name = agregar(document, signatory, CAC_NS, "cac:PartyName");
        texto(document, name, CBC_NS, "cbc:Name", empresa.getRazonSocial());
        Element attachment = agregar(document, signature, CAC_NS, "cac:DigitalSignatureAttachment");
        Element reference = agregar(document, attachment, CAC_NS, "cac:ExternalReference");
        texto(document, reference, CBC_NS, "cbc:URI", "#signatureKG");
    }

    private void proveedor(Document document, Element root, Empresa empresa) {
        Element supplier = agregar(document, root, CAC_NS, "cac:AccountingSupplierParty");
        texto(document, supplier, CBC_NS, "cbc:CustomerAssignedAccountID", empresa.getRuc());
        texto(document, supplier, CBC_NS, "cbc:AdditionalAccountID", "6");
        Element party = agregar(document, supplier, CAC_NS, "cac:Party");
        Element legal = agregar(document, party, CAC_NS, "cac:PartyLegalEntity");
        texto(document, legal, CBC_NS, "cbc:RegistrationName", empresa.getRazonSocial());
    }

    private void linea(Document document, Element root, int numero, Comprobante comprobante) {
        Element line = agregar(document, root, SAC_NS, "sac:SummaryDocumentsLine");
        texto(document, line, CBC_NS, "cbc:LineID", Integer.toString(numero));
        texto(document, line, CBC_NS, "cbc:DocumentTypeCode", "03");
        texto(document, line, CBC_NS, "cbc:ID", comprobante.getNumeroCompleto());
        cliente(document, line, comprobante.getVenta().getCliente());

        Element status = agregar(document, line, CAC_NS, "cac:Status");
        texto(document, status, CBC_NS, "cbc:ConditionCode", "1");
        importe(document, line, SAC_NS, "sac:TotalAmount", comprobante.getTotal());

        Element payment = agregar(document, line, SAC_NS, "sac:BillingPayment");
        importe(document, payment, CBC_NS, "cbc:PaidAmount", comprobante.getSubtotal());
        texto(document, payment, CBC_NS, "cbc:InstructionID", "01");

        Element taxTotal = agregar(document, line, CAC_NS, "cac:TaxTotal");
        importe(document, taxTotal, CBC_NS, "cbc:TaxAmount", comprobante.getIgv());
        Element subtotal = agregar(document, taxTotal, CAC_NS, "cac:TaxSubtotal");
        importe(document, subtotal, CBC_NS, "cbc:TaxAmount", comprobante.getIgv());
        Element scheme = agregar(document, subtotal, CAC_NS, "cac:TaxCategory");
        Element taxScheme = agregar(document, scheme, CAC_NS, "cac:TaxScheme");
        texto(document, taxScheme, CBC_NS, "cbc:ID", "1000");
        texto(document, taxScheme, CBC_NS, "cbc:Name", "IGV");
        texto(document, taxScheme, CBC_NS, "cbc:TaxTypeCode", "VAT");
    }

    private void cliente(Document document, Element line, Cliente cliente) {
        Element customer = agregar(document, line, CAC_NS, "cac:AccountingCustomerParty");
        boolean identificado = cliente != null && cliente.getNumeroDocumento() != null
            && !cliente.getNumeroDocumento().isBlank();
        texto(
            document,
            customer,
            CBC_NS,
            "cbc:CustomerAssignedAccountID",
            identificado ? cliente.getNumeroDocumento() : "-"
        );
        texto(
            document,
            customer,
            CBC_NS,
            "cbc:AdditionalAccountID",
            identificado
                ? cliente.getTipoDocumento() == TipoDocumentoCliente.RUC ? "6" : "1"
                : "-"
        );
    }

    private Element importe(
        Document document,
        Element parent,
        String namespace,
        String name,
        BigDecimal value
    ) {
        Element amount = texto(
            document,
            parent,
            namespace,
            name,
            value.setScale(2, RoundingMode.HALF_UP).toPlainString()
        );
        amount.setAttribute("currencyID", "PEN");
        return amount;
    }

    private Element agregar(Document document, Element parent, String namespace, String name) {
        Element element = document.createElementNS(namespace, name);
        parent.appendChild(element);
        return element;
    }

    private Element texto(
        Document document,
        Element parent,
        String namespace,
        String name,
        String value
    ) {
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
