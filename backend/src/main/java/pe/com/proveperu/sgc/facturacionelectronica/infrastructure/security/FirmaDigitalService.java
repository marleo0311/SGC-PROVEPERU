package pe.com.proveperu.sgc.facturacionelectronica.infrastructure.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.security.CertificadoDigitalProvider.CredencialFirma;

@Service
public class FirmaDigitalService {

    private final CertificadoDigitalProvider certificadoProvider;

    public FirmaDigitalService(CertificadoDigitalProvider certificadoProvider) {
        this.certificadoProvider = certificadoProvider;
    }

    public byte[] firmar(byte[] xml) {
        try {
            CredencialFirma credencial = certificadoProvider.cargar();
            Document document = parsear(xml);
            Node extensionContent = document
                .getElementsByTagNameNS(
                    "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2",
                    "ExtensionContent"
                )
                .item(0);
            if (extensionContent == null) {
                throw new IllegalArgumentException("El XML UBL no contiene ExtensionContent");
            }

            XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");
            Reference reference = factory.newReference(
                "",
                factory.newDigestMethod(DigestMethod.SHA256, null),
                List.of(
                    factory.newTransform(Transform.ENVELOPED, (javax.xml.crypto.dsig.spec.TransformParameterSpec) null),
                    factory.newTransform(CanonicalizationMethod.INCLUSIVE, (javax.xml.crypto.dsig.spec.TransformParameterSpec) null)
                ),
                null,
                null
            );
            SignedInfo signedInfo = factory.newSignedInfo(
                factory.newCanonicalizationMethod(
                    CanonicalizationMethod.INCLUSIVE,
                    (javax.xml.crypto.dsig.spec.C14NMethodParameterSpec) null
                ),
                factory.newSignatureMethod(SignatureMethod.RSA_SHA256, null),
                List.of(reference)
            );
            KeyInfoFactory keyInfoFactory = factory.getKeyInfoFactory();
            KeyInfo keyInfo = keyInfoFactory.newKeyInfo(List.of(
                keyInfoFactory.newX509Data(List.of(
                    credencial.certificate().getSubjectX500Principal().getName(),
                    credencial.certificate()
                ))
            ));
            DOMSignContext context = new DOMSignContext(credencial.privateKey(), extensionContent);
            context.setDefaultNamespacePrefix("ds");
            factory.newXMLSignature(signedInfo, keyInfo, null, "signatureKG", null).sign(context);
            return serializar(document);
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo firmar digitalmente el XML UBL", exception);
        }
    }

    private Document parsear(byte[] xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
    }

    private byte[] serializar(Document document) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        var transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.INDENT, "no");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(document), new StreamResult(output));
        return output.toByteArray();
    }
}
