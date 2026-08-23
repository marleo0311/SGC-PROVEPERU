package pe.com.proveperu.sgc.shared.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Calcula el desglose tributario cuando los precios del catálogo ya son precios
 * finales para el cliente.
 */
public final class CalculoTributario {

    private static final int ESCALA_DINERO = 2;
    private static final BigDecimal FACTOR_PRECIO_CON_IGV = new BigDecimal("1.18");

    private CalculoTributario() {
    }

    public static Totales desdePrecioFinal(
        BigDecimal importeFinal,
        boolean aplicarIgv
    ) {
        BigDecimal total = importeFinal.setScale(
            ESCALA_DINERO,
            RoundingMode.HALF_UP
        );
        if (!aplicarIgv) {
            return new Totales(
                total,
                BigDecimal.ZERO.setScale(ESCALA_DINERO),
                total
            );
        }

        BigDecimal baseImponible = total.divide(
            FACTOR_PRECIO_CON_IGV,
            ESCALA_DINERO,
            RoundingMode.HALF_UP
        );
        BigDecimal igv = total.subtract(baseImponible).setScale(
            ESCALA_DINERO,
            RoundingMode.HALF_UP
        );
        return new Totales(baseImponible, igv, total);
    }

    public record Totales(
        BigDecimal subtotal,
        BigDecimal igv,
        BigDecimal total
    ) {
    }
}
