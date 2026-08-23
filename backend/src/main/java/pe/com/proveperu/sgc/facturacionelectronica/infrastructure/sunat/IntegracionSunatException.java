package pe.com.proveperu.sgc.facturacionelectronica.infrastructure.sunat;

public class IntegracionSunatException extends RuntimeException {

    public IntegracionSunatException(String message) {
        super(message);
    }

    public IntegracionSunatException(String message, Throwable cause) {
        super(message, cause);
    }
}
