package pe.com.proveperu.sgc.devolucion.api.dto;

import java.util.List;
import pe.com.proveperu.sgc.devolucion.domain.model.Devolucion;

public record DevolucionResponse(
    DevolucionResumenResponse devolucion,
    List<DetalleDevolucionResponse> items,
    ReembolsoDevolucionResponse reembolso
) {
    public static DevolucionResponse from(Devolucion devolucion) {
        ReembolsoDevolucionResponse reembolso = devolucion.getReembolso() == null
            ? null
            : ReembolsoDevolucionResponse.from(devolucion.getReembolso());
        return new DevolucionResponse(
            DevolucionResumenResponse.from(devolucion),
            devolucion.getDetalles().stream()
                .map(DetalleDevolucionResponse::from)
                .toList(),
            reembolso
        );
    }
}
