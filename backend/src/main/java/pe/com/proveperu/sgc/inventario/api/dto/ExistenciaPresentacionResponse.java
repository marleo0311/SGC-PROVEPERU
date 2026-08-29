package pe.com.proveperu.sgc.inventario.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import pe.com.proveperu.sgc.inventario.domain.model.ExistenciaPresentacion;

public record ExistenciaPresentacionResponse(
    Long id,
    String codigo,
    Long idSede,
    String sede,
    Long idProducto,
    String codigoProducto,
    String producto,
    Long idPresentacionProducto,
    String presentacion,
    Long idUnidadPresentacion,
    String codigoUnidadPresentacion,
    String nombreUnidadPresentacion,
    Long idUnidadBase,
    String codigoUnidadBase,
    String nombreUnidadBase,
    BigDecimal cantidadInicialBase,
    BigDecimal cantidadDisponibleBase,
    String estado,
    Instant fechaIngreso,
    Instant fechaApertura
) {
    public static ExistenciaPresentacionResponse from(ExistenciaPresentacion existencia) {
        var presentacion = existencia.getPresentacion();
        var producto = presentacion.getProducto();
        return new ExistenciaPresentacionResponse(
            existencia.getId(), existencia.getCodigo(), existencia.getSede().getId(),
            existencia.getSede().getNombre(), producto.getId(), producto.getCodigoInterno(),
            producto.getNombre(), presentacion.getId(), presentacion.getNombre(),
            presentacion.getUnidadMedida().getId(), presentacion.getUnidadMedida().getCodigo(),
            presentacion.getUnidadMedida().getNombre(), producto.getUnidadBase().getId(),
            producto.getUnidadBase().getCodigo(), producto.getUnidadBase().getNombre(),
            existencia.getCantidadInicialBase(), existencia.getCantidadDisponibleBase(),
            existencia.getEstado().name(), existencia.getFechaIngreso(),
            existencia.getFechaApertura()
        );
    }
}
