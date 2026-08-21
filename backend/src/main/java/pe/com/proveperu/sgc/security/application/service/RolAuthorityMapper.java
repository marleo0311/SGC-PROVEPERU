package pe.com.proveperu.sgc.security.application.service;

import java.text.Normalizer;
import java.util.Locale;

public final class RolAuthorityMapper {

    private RolAuthorityMapper() {
    }

    public static String normalizar(String nombre) {
        String sinAcentos = Normalizer.normalize(nombre, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");

        return sinAcentos.trim()
            .toUpperCase(Locale.ROOT)
            .replaceAll("[^A-Z0-9]+", "_")
            .replaceAll("^_+|_+$", "");
    }
}
