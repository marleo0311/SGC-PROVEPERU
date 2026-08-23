package pe.com.proveperu.sgc.cliente.application.service;

public final class ValidadorRuc {

    private static final int[] FACTORES = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    private ValidadorRuc() {
    }

    public static boolean esValido(String ruc) {
        if (ruc == null || !ruc.matches("^[0-9]{11}$")) {
            return false;
        }
        int suma = 0;
        for (int indice = 0; indice < FACTORES.length; indice++) {
            suma += Character.digit(ruc.charAt(indice), 10) * FACTORES[indice];
        }
        int digitoEsperado = 11 - (suma % 11);
        if (digitoEsperado >= 10) {
            digitoEsperado -= 10;
        }
        return digitoEsperado == Character.digit(ruc.charAt(10), 10);
    }
}
