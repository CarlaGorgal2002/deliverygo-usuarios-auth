package ar.edu.uade.api.deliverygo.usuarios.security;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Set;

// Las únicas operaciones que no exigen credenciales: registro, login y recuperación de contraseña.
// Los filtros las saltean para que un token viejo guardado en el navegador no las rompa.
final class RutasPublicas {

    static final String REGISTRO = "/api/v1/auth/register";
    static final String LOGIN = "/api/v1/auth/login";
    static final String RECUPERACION_SOLICITUD = "/api/v1/auth/password/forgot";
    static final String RECUPERACION_RESTABLECIMIENTO = "/api/v1/auth/password/reset";

    private static final Set<String> POST_PUBLICOS =
            Set.of(REGISTRO, LOGIN, RECUPERACION_SOLICITUD, RECUPERACION_RESTABLECIMIENTO);

    private RutasPublicas() {}

    static boolean esPublica(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && POST_PUBLICOS.contains(request.getRequestURI());
    }

    static String[] postPublicos() {
        return POST_PUBLICOS.toArray(String[]::new);
    }
}
