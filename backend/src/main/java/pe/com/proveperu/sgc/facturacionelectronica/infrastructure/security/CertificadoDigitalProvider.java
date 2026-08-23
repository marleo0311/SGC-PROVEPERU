package pe.com.proveperu.sgc.facturacionelectronica.infrastructure.security;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import org.springframework.stereotype.Component;
import pe.com.proveperu.sgc.facturacionelectronica.infrastructure.config.SunatProperties;
import pe.com.proveperu.sgc.shared.application.exception.ReglaNegocioException;

@Component
public class CertificadoDigitalProvider {

    private final SunatProperties properties;

    public CertificadoDigitalProvider(SunatProperties properties) {
        this.properties = properties;
    }

    public CredencialFirma cargar() {
        if (properties.getCertificadoRuta() == null || properties.getCertificadoRuta().isBlank()) {
            throw new ReglaNegocioException(
                "Configura SUNAT_CERTIFICATE_PATH con la ruta local del certificado .p12"
            );
        }
        if (properties.getCertificadoClave() == null || properties.getCertificadoClave().isBlank()) {
            throw new ReglaNegocioException(
                "Configura SUNAT_CERTIFICATE_PASSWORD sin publicarla en el repositorio"
            );
        }
        Path ruta = Path.of(properties.getCertificadoRuta()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(ruta)) {
            throw new ReglaNegocioException("No se encontró el certificado digital configurado");
        }
        char[] clave = properties.getCertificadoClave().toCharArray();
        try (InputStream input = Files.newInputStream(ruta)) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(input, clave);
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (!keyStore.isKeyEntry(alias)) {
                    continue;
                }
                PrivateKey privateKey = (PrivateKey) keyStore.getKey(alias, clave);
                X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);
                certificate.checkValidity();
                return new CredencialFirma(privateKey, certificate);
            }
            throw new ReglaNegocioException("El archivo .p12 no contiene una llave privada utilizable");
        } catch (ReglaNegocioException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ReglaNegocioException(
                "No se pudo abrir el certificado digital; revisa su formato y contraseña"
            );
        } finally {
            java.util.Arrays.fill(clave, '\0');
        }
    }

    public record CredencialFirma(
        PrivateKey privateKey,
        X509Certificate certificate
    ) {
    }
}
