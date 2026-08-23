package pe.com.proveperu.sgc.facturacionelectronica.application.service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
import pe.com.proveperu.sgc.venta.domain.model.DetalleVenta;
import pe.com.proveperu.sgc.venta.domain.model.TipoComprobanteVenta;

@Service
public class GeneradorUblService {

    static final String INVOICE_NS = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2";
    static final String CAC_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    static final String CBC_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
    static final String EXT_NS = "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";
    static final String DS_NS = "http://www.w3.org/2000/09/xmldsig#";
    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final BigDecimal IGV_FACTOR = new BigDecimal("1.18");

    public DocumentoUbl generar(Comprobante comprobante, Empresa empresa) {
        validar(comprobante, empresa);
        try {
            Document document = documento();
            Element invoice = document.createElementNS(INVOICE_NS, "Invoice");
            invoice.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns", INVOICE_NS);
            invoice.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:cac", CAC_NS);
            invoice.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:cbc", CBC_NS);
            invoice.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:ext", EXT_NS);
            invoice.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, "xmlns:ds", DS_NS);
            document.appendChild(invoice);

            extensionFirma(document, invoice);
            texto(document, invoice, CBC_NS, "cbc:UBLVersionID", "2.1");
            texto(document, invoice, CBC_NS, "cbc:CustomizationID", "2.0");
            Element profile = texto(document, invoice, CBC_NS, "cbc:ProfileID", "0101");
            profile.setAttribute("schemeName", "SUNAT:Identificador de Tipo de Operación");
            profile.setAttribute("schemeAgencyName", "PE:SUNAT");
            profile.setAttribute("schemeURI", "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo17");
            texto(document, invoice, CBC_NS, "cbc:ID", comprobante.getNumeroCompleto());
            var fecha = comprobante.getFechaEmision().atZone(LIMA);
            texto(document, invoice, CBC_NS, "cbc:IssueDate", fecha.toLocalDate().toString());
            texto(
                document,
                invoice,
                CBC_NS,
                "cbc:IssueTime",
                fecha.format(DateTimeFormatter.ofPattern("HH:mm:ssXXX"))
            );
            Element tipo = texto(
                document,
                invoice,
                CBC_NS,
                "cbc:InvoiceTypeCode",
                codigoTipo(comprobante.getTipo())
            );
            tipo.setAttribute("listAgencyName", "PE:SUNAT");
            tipo.setAttribute("listID", "0101");
            tipo.setAttribute("listName", "Tipo de Documento");
            tipo.setAttribute("listSchemeURI", "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo51");
            tipo.setAttribute("listURI", "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo01");
            tipo.setAttribute("name", "Tipo de Operacion");
            Element nota = texto(
                document,
                invoice,
                CBC_NS,
                "cbc:Note",
                MontoEnLetras.soles(comprobante.getTotal())
            );
            nota.setAttribute("languageLocaleID", "1000");
            Element moneda = texto(document, invoice, CBC_NS, "cbc:DocumentCurrencyCode", "PEN");
            moneda.setAttribute("listID", "ISO 4217 Alpha");
            moneda.setAttribute("listName", "Currency");
            moneda.setAttribute("listAgencyName", "United Nations Economic Commission for Europe");

            firmaDeclarada(document, invoice, comprobante, empresa);
            proveedor(document, invoice, empresa, comprobante);
            cliente(document, invoice, comprobante.getVenta().getCliente());
            totales(document, invoice, comprobante);

            List<LineaTributaria> lineas = distribuir(comprobante);
            for (int index = 0; index < lineas.size(); index++) {
                linea(document, invoice, index + 1, lineas.get(index));
            }

            String nombreBase = "%s-%s-%s".formatted(
                empresa.getRuc(),
                codigoTipo(comprobante.getTipo()),
                comprobante.getNumeroCompleto()
            );
            return new DocumentoUbl(nombreBase, serializar(document));
        } catch (ReglaNegocioException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo generar el XML UBL 2.1", exception);
        }
    }

    private void validar(Comprobante comprobante, Empresa empresa) {
        if (comprobante.getTipo() == TipoComprobanteVenta.NOTA_VENTA) {
            throw new ReglaNegocioException("Las notas de venta son documentos internos y no se envían a SUNAT");
        }
        if (empresa == null || empresa.getRuc() == null || !empresa.getRuc().matches("\\d{11}")) {
            throw new ReglaNegocioException("Configura un RUC válido para la empresa emisora");
        }
        if (empresa.getRazonSocial() == null || empresa.getRazonSocial().isBlank()) {
            throw new ReglaNegocioException("Configura la razón social de la empresa emisora");
        }
        if (comprobante.getIgv() == null || comprobante.getIgv().signum() <= 0) {
            throw new ReglaNegocioException(
                "La primera versión SUNAT admite únicamente operaciones gravadas con IGV"
            );
        }
        if (comprobante.getTipo() == TipoComprobanteVenta.FACTURA) {
            Cliente cliente = comprobante.getVenta().getCliente();
            if (cliente == null || cliente.getTipoDocumento() != TipoDocumentoCliente.RUC) {
                throw new ReglaNegocioException("La factura electrónica requiere un cliente con RUC");
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

    private void extensionFirma(Document document, Element invoice) {
        Element extensions = agregar(document, invoice, EXT_NS, "ext:UBLExtensions");
        Element extension = agregar(document, extensions, EXT_NS, "ext:UBLExtension");
        agregar(document, extension, EXT_NS, "ext:ExtensionContent");
    }

    private void firmaDeclarada(
        Document document,
        Element invoice,
        Comprobante comprobante,
        Empresa empresa
    ) {
        Element signature = agregar(document, invoice, CAC_NS, "cac:Signature");
        texto(document, signature, CBC_NS, "cbc:ID", "SIGN-" + comprobante.getNumeroCompleto());
        Element signatory = agregar(document, signature, CAC_NS, "cac:SignatoryParty");
        Element identification = agregar(document, signatory, CAC_NS, "cac:PartyIdentification");
        texto(document, identification, CBC_NS, "cbc:ID", empresa.getRuc());
        Element name = agregar(document, signatory, CAC_NS, "cac:PartyName");
        texto(document, name, CBC_NS, "cbc:Name", empresa.getRazonSocial());
        Element attachment = agregar(
            document,
            signature,
            CAC_NS,
            "cac:DigitalSignatureAttachment"
        );
        Element reference = agregar(document, attachment, CAC_NS, "cac:ExternalReference");
        texto(document, reference, CBC_NS, "cbc:URI", "#signatureKG");
    }

    private void proveedor(
        Document document,
        Element invoice,
        Empresa empresa,
        Comprobante comprobante
    ) {
        Element supplier = agregar(document, invoice, CAC_NS, "cac:AccountingSupplierParty");
        Element party = agregar(document, supplier, CAC_NS, "cac:Party");
        Element identification = agregar(document, party, CAC_NS, "cac:PartyIdentification");
        documentoIdentidad(document, identification, empresa.getRuc(), "6");
        if (empresa.getNombreComercial() != null && !empresa.getNombreComercial().isBlank()) {
            Element partyName = agregar(document, party, CAC_NS, "cac:PartyName");
            texto(document, partyName, CBC_NS, "cbc:Name", empresa.getNombreComercial());
        }

        Element legal = agregar(document, party, CAC_NS, "cac:PartyLegalEntity");
        texto(document, legal, CBC_NS, "cbc:RegistrationName", empresa.getRazonSocial());

        String direccion = comprobante.getVenta().getSede().getDireccion();
        if (direccion == null || direccion.isBlank()) {
            direccion = empresa.getDireccion();
        }

        Element address = agregar(document, legal, CAC_NS, "cac:RegistrationAddress");
        textoSiExiste(document, address, CBC_NS, "cbc:ID", empresa.getUbigeo());
        Element addressTypeCode = texto(
            document,
            address,
            CBC_NS,
            "cbc:AddressTypeCode",
            comprobante.getVenta().getSede().getCodigoEstablecimientoSunat()
        );
        addressTypeCode.setAttribute("listAgencyName", "PE:SUNAT");
        addressTypeCode.setAttribute("listName", "Establecimientos anexos");
        textoSiExiste(document, address, CBC_NS, "cbc:CityName", empresa.getProvincia());
        textoSiExiste(document, address, CBC_NS, "cbc:CountrySubentity", empresa.getDepartamento());
        textoSiExiste(document, address, CBC_NS, "cbc:District", empresa.getDistrito());
        if (direccion != null && !direccion.isBlank()) {
            Element line = agregar(document, address, CAC_NS, "cac:AddressLine");
            texto(document, line, CBC_NS, "cbc:Line", direccion);
        }
        Element country = agregar(document, address, CAC_NS, "cac:Country");
        Element code = texto(
            document,
            country,
            CBC_NS,
            "cbc:IdentificationCode",
            empresa.getCodigoPais() == null ? "PE" : empresa.getCodigoPais()
        );
        code.setAttribute("listID", "ISO 3166-1");
        code.setAttribute("listAgencyName", "United Nations Economic Commission for Europe");
        code.setAttribute("listName", "Country");

    }

    private void cliente(Document document, Element invoice, Cliente cliente) {
        Element customer = agregar(document, invoice, CAC_NS, "cac:AccountingCustomerParty");
        Element party = agregar(document, customer, CAC_NS, "cac:Party");
        Element identification = agregar(document, party, CAC_NS, "cac:PartyIdentification");
        String numero = cliente == null ? "-" : cliente.getNumeroDocumento();
        String tipo = cliente == null
            ? "0"
            : cliente.getTipoDocumento() == TipoDocumentoCliente.RUC ? "6" : "1";
        documentoIdentidad(document, identification, numero, tipo);
        Element legal = agregar(document, party, CAC_NS, "cac:PartyLegalEntity");
        texto(
            document,
            legal,
            CBC_NS,
            "cbc:RegistrationName",
            cliente == null ? "CLIENTE VARIOS" : nombreCliente(cliente)
        );
    }

    private void documentoIdentidad(
        Document document,
        Element parent,
        String numero,
        String tipo
    ) {
        Element id = texto(document, parent, CBC_NS, "cbc:ID", numero);
        id.setAttribute("schemeID", tipo);
        id.setAttribute("schemeName", "Documento de Identidad");
        id.setAttribute("schemeAgencyName", "PE:SUNAT");
        id.setAttribute("schemeURI", "urn:pe:gob:sunat:cpe:see:gem:catalogos:catalogo06");
    }

    private void totales(Document document, Element invoice, Comprobante comprobante) {
        Element taxTotal = agregar(document, invoice, CAC_NS, "cac:TaxTotal");
        importe(document, taxTotal, "cbc:TaxAmount", comprobante.getIgv());
        Element subtotal = agregar(document, taxTotal, CAC_NS, "cac:TaxSubtotal");
        importe(document, subtotal, "cbc:TaxableAmount", comprobante.getSubtotal());
        importe(document, subtotal, "cbc:TaxAmount", comprobante.getIgv());
        categoriaIgv(document, subtotal, false);

        Element monetary = agregar(document, invoice, CAC_NS, "cac:LegalMonetaryTotal");
        importe(document, monetary, "cbc:LineExtensionAmount", comprobante.getSubtotal());
        importe(document, monetary, "cbc:TaxInclusiveAmount", comprobante.getTotal());
        importe(document, monetary, "cbc:PayableAmount", comprobante.getTotal());
    }

    private void linea(
        Document document,
        Element invoice,
        int numero,
        LineaTributaria linea
    ) {
        DetalleVenta detalle = linea.detalle();
        Element invoiceLine = agregar(document, invoice, CAC_NS, "cac:InvoiceLine");
        texto(document, invoiceLine, CBC_NS, "cbc:ID", Integer.toString(numero));
        Element quantity = texto(
            document,
            invoiceLine,
            CBC_NS,
            "cbc:InvoicedQuantity",
            detalle.getCantidad().stripTrailingZeros().toPlainString()
        );
        quantity.setAttribute(
            "unitCode",
            detalle.getUnidadMedida().getCodigoSunat() == null
                ? "NIU"
                : detalle.getUnidadMedida().getCodigoSunat()
        );
        quantity.setAttribute("unitCodeListID", "UN/ECE rec 20");
        quantity.setAttribute("unitCodeListAgencyName", "United Nations Economic Commission for Europe");
        importe(document, invoiceLine, "cbc:LineExtensionAmount", linea.base());

        Element pricing = agregar(document, invoiceLine, CAC_NS, "cac:PricingReference");
        Element alternative = agregar(
            document,
            pricing,
            CAC_NS,
            "cac:AlternativeConditionPrice"
        );
        importe(document, alternative, "cbc:PriceAmount", detalle.getPrecioUnitario());
        texto(document, alternative, CBC_NS, "cbc:PriceTypeCode", "01");

        if (detalle.getDescuento() != null && detalle.getDescuento().signum() > 0) {
            Element allowance = agregar(document, invoiceLine, CAC_NS, "cac:AllowanceCharge");
            texto(document, allowance, CBC_NS, "cbc:ChargeIndicator", "false");
            texto(document, allowance, CBC_NS, "cbc:AllowanceChargeReasonCode", "00");
            importe(document, allowance, "cbc:Amount", linea.descuentoBase());
            importe(
                document,
                allowance,
                "cbc:BaseAmount",
                linea.base().add(linea.descuentoBase())
            );
        }

        Element taxTotal = agregar(document, invoiceLine, CAC_NS, "cac:TaxTotal");
        importe(document, taxTotal, "cbc:TaxAmount", linea.igv());
        Element taxSubtotal = agregar(document, taxTotal, CAC_NS, "cac:TaxSubtotal");
        importe(document, taxSubtotal, "cbc:TaxableAmount", linea.base());
        importe(document, taxSubtotal, "cbc:TaxAmount", linea.igv());
        categoriaIgv(document, taxSubtotal, true);

        Element item = agregar(document, invoiceLine, CAC_NS, "cac:Item");
        texto(document, item, CBC_NS, "cbc:Description", detalle.getProducto().getNombre());
        Element seller = agregar(document, item, CAC_NS, "cac:SellersItemIdentification");
        texto(document, seller, CBC_NS, "cbc:ID", detalle.getProducto().getCodigoInterno());

        Element price = agregar(document, invoiceLine, CAC_NS, "cac:Price");
        importe(document, price, "cbc:PriceAmount", linea.valorUnitario());
    }

    private void categoriaIgv(Document document, Element parent, boolean conAfectacion) {
        Element category = agregar(document, parent, CAC_NS, "cac:TaxCategory");
        if (conAfectacion) {
            texto(document, category, CBC_NS, "cbc:Percent", "18.00");
            texto(document, category, CBC_NS, "cbc:TaxExemptionReasonCode", "10");
        }
        Element scheme = agregar(document, category, CAC_NS, "cac:TaxScheme");
        Element id = texto(document, scheme, CBC_NS, "cbc:ID", "1000");
        id.setAttribute("schemeID", "UN/ECE 5153");
        id.setAttribute("schemeAgencyID", "6");
        texto(document, scheme, CBC_NS, "cbc:Name", "IGV");
        texto(document, scheme, CBC_NS, "cbc:TaxTypeCode", "VAT");
    }

    private List<LineaTributaria> distribuir(Comprobante comprobante) {
        List<DetalleVenta> detalles = comprobante.getVenta().getDetalles();
        List<LineaTributaria> resultado = new ArrayList<>();
        BigDecimal baseAcumulada = BigDecimal.ZERO;
        BigDecimal igvAcumulado = BigDecimal.ZERO;
        for (int index = 0; index < detalles.size(); index++) {
            DetalleVenta detalle = detalles.get(index);
            boolean ultima = index == detalles.size() - 1;
            BigDecimal base = ultima
                ? comprobante.getSubtotal().subtract(baseAcumulada)
                : detalle.getSubtotal().divide(IGV_FACTOR, 2, RoundingMode.HALF_UP);
            BigDecimal igv = ultima
                ? comprobante.getIgv().subtract(igvAcumulado)
                : detalle.getSubtotal().subtract(base).setScale(2, RoundingMode.HALF_UP);
            BigDecimal descuentoBase = detalle.getDescuento() == null
                ? BigDecimal.ZERO
                : detalle.getDescuento().divide(IGV_FACTOR, 2, RoundingMode.HALF_UP);
            BigDecimal valorUnitario = detalle.getPrecioUnitario()
                .divide(IGV_FACTOR, 10, RoundingMode.HALF_UP);
            resultado.add(new LineaTributaria(
                detalle,
                base.setScale(2, RoundingMode.HALF_UP),
                igv.setScale(2, RoundingMode.HALF_UP),
                descuentoBase,
                valorUnitario
            ));
            baseAcumulada = baseAcumulada.add(base);
            igvAcumulado = igvAcumulado.add(igv);
        }
        return resultado;
    }

    private String nombreCliente(Cliente cliente) {
        if (cliente.getRazonSocial() != null && !cliente.getRazonSocial().isBlank()) {
            return cliente.getRazonSocial();
        }
        String nombre = "%s %s".formatted(
            cliente.getNombres() == null ? "" : cliente.getNombres(),
            cliente.getApellidos() == null ? "" : cliente.getApellidos()
        ).strip();
        return nombre.isBlank() ? cliente.getNumeroDocumento() : nombre;
    }

    private String codigoTipo(TipoComprobanteVenta tipo) {
        return switch (tipo) {
            case FACTURA -> "01";
            case BOLETA -> "03";
            case NOTA_VENTA -> throw new ReglaNegocioException(
                "La nota de venta no tiene código de comprobante electrónico"
            );
        };
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

    private void textoSiExiste(
        Document document,
        Element parent,
        String namespace,
        String name,
        String value
    ) {
        if (value != null && !value.isBlank()) {
            texto(document, parent, namespace, name, value);
        }
    }

    private Element importe(Document document, Element parent, String name, BigDecimal value) {
        Element amount = texto(
            document,
            parent,
            CBC_NS,
            name,
            value.setScale(2, RoundingMode.HALF_UP).toPlainString()
        );
        amount.setAttribute("currencyID", "PEN");
        return amount;
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

    private record LineaTributaria(
        DetalleVenta detalle,
        BigDecimal base,
        BigDecimal igv,
        BigDecimal descuentoBase,
        BigDecimal valorUnitario
    ) {
    }
}
