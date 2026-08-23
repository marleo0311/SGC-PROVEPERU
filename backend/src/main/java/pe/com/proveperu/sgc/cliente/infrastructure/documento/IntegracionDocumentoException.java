package pe.com.proveperu.sgc.cliente.infrastructure.documento;

public class IntegracionDocumentoException extends RuntimeException {

    public IntegracionDocumentoException(String message) {
        super(message);
    }

    public IntegracionDocumentoException(String message, Throwable cause) {
        super(message, cause);
    }
}
