package pe.com.proveperu.sgc.facturacionelectronica.infrastructure.sunat;

import lombok.Getter;

@Getter
public class RechazoSunatException extends IntegracionSunatException {

    private final String codigo;

    public RechazoSunatException(String codigo, String message) {
        super(message);
        this.codigo = codigo;
    }
}
