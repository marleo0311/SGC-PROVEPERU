package pe.com.proveperu.sgc.facturacionelectronica.application.dto;

import java.util.List;

public record ResultadoCdr(
    String codigo,
    String descripcion,
    List<String> observaciones
) {
    public boolean aceptado() {
        return "0".equals(codigo);
    }
}
