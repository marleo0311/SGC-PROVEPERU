package pe.com.proveperu.sgc.compra.api.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

public record RecepcionCompraItemRequest(
    @Positive(message = "El detalle de compra debe ser válido")
    Long idDetalleCompra,

    @Positive(message = "El producto debe ser válido")
    Long idProducto,

    @NotNull(message = "La cantidad recibida es obligatoria")
    @DecimalMin(value = "0.001", message = "La cantidad recibida debe ser mayor que cero")
    @Digits(
        integer = 11,
        fraction = 3,
        message = "La cantidad recibida admite hasta 11 enteros y 3 decimales"
    )
    BigDecimal cantidadRecibida,

    Boolean conforme,

    @Size(max = 200, message = "Solo se pueden informar 200 bultos por producto")
    List<@NotNull @DecimalMin("0.001") @Digits(integer = 11, fraction = 3) BigDecimal> contenidosBase,

    @Size(max = 250, message = "La observación del producto no puede superar 250 caracteres")
    String observacion
) {
    @AssertTrue(message = "Debe indicar el detalle de compra o el producto recibido")
    public boolean isReferenciaInformada() {
        return idDetalleCompra != null || idProducto != null;
    }

    public boolean conformeEfectivo() {
        return conforme == null || conforme;
    }

    public RecepcionCompraItemRequest(
        Long idDetalleCompra,
        Long idProducto,
        BigDecimal cantidadRecibida,
        Boolean conforme,
        String observacion
    ) {
        this(idDetalleCompra, idProducto, cantidadRecibida, conforme, null, observacion);
    }
}
