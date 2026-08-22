package pe.com.proveperu.sgc.caja.api.dto;

import pe.com.proveperu.sgc.caja.domain.model.Caja;
import pe.com.proveperu.sgc.caja.domain.model.EstadoCaja;

public record CajaResponse(
    Long id,
    Long idSede,
    String sede,
    String nombre,
    EstadoCaja estado
) {
    public static CajaResponse from(Caja caja) {
        return new CajaResponse(
            caja.getId(),
            caja.getSede().getId(),
            caja.getSede().getNombre(),
            caja.getNombre(),
            caja.getEstado()
        );
    }
}
