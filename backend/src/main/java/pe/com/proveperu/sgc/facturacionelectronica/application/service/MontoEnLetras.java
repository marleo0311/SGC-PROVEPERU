package pe.com.proveperu.sgc.facturacionelectronica.application.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class MontoEnLetras {

    private static final String[] UNIDADES = {
        "", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE"
    };
    private static final String[] ESPECIALES = {
        "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE",
        "DIECISEIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE"
    };
    private static final String[] DECENAS = {
        "", "", "VEINTE", "TREINTA", "CUARENTA", "CINCUENTA",
        "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"
    };
    private static final String[] CENTENAS = {
        "", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS",
        "QUINIENTOS", "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"
    };

    private MontoEnLetras() {
    }

    static String soles(BigDecimal monto) {
        BigDecimal normalizado = monto.setScale(2, RoundingMode.HALF_UP);
        long entero = normalizado.longValue();
        int centimos = normalizado.remainder(BigDecimal.ONE)
            .movePointRight(2)
            .abs()
            .intValue();
        String letras = entero == 0 ? "CERO" : numero(entero).replace("UNO", "UN");
        String moneda = entero == 1 ? "SOL" : "SOLES";
        return "SON %s CON %02d/100 %s".formatted(letras, centimos, moneda);
    }

    private static String numero(long valor) {
        if (valor < 1_000) {
            return centenas((int) valor);
        }
        if (valor < 1_000_000) {
            long miles = valor / 1_000;
            long resto = valor % 1_000;
            return unir(miles == 1 ? "MIL" : numero(miles) + " MIL", numero(resto));
        }
        if (valor < 1_000_000_000_000L) {
            long millones = valor / 1_000_000;
            long resto = valor % 1_000_000;
            String prefijo = millones == 1
                ? "UN MILLON"
                : numero(millones) + " MILLONES";
            return unir(prefijo, numero(resto));
        }
        throw new IllegalArgumentException("El importe supera el máximo soportado");
    }

    private static String centenas(int valor) {
        if (valor == 0) {
            return "";
        }
        if (valor == 100) {
            return "CIEN";
        }
        int centenas = valor / 100;
        int resto = valor % 100;
        return unir(CENTENAS[centenas], decenas(resto));
    }

    private static String decenas(int valor) {
        if (valor < 10) {
            return UNIDADES[valor];
        }
        if (valor < 20) {
            return ESPECIALES[valor - 10];
        }
        if (valor < 30) {
            return valor == 20 ? "VEINTE" : "VEINTI" + UNIDADES[valor - 20];
        }
        int decena = valor / 10;
        int unidad = valor % 10;
        return unidad == 0
            ? DECENAS[decena]
            : DECENAS[decena] + " Y " + UNIDADES[unidad];
    }

    private static String unir(String izquierda, String derecha) {
        if (izquierda == null || izquierda.isBlank()) {
            return derecha == null ? "" : derecha;
        }
        if (derecha == null || derecha.isBlank()) {
            return izquierda;
        }
        return izquierda + " " + derecha;
    }
}
