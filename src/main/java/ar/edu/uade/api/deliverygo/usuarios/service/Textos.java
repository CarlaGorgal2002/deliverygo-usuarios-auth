package ar.edu.uade.api.deliverygo.usuarios.service;

import java.util.Locale;

// Normalizaciones de texto compartidas por los servicios.
final class Textos {

    private Textos() {}

    // DC-02: sin espacios exteriores y en minúsculas, para comparar y persistir el email.
    static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    // DC-03: un opcional ausente, null o en blanco se guarda como "sin valor".
    static String recortarONulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
