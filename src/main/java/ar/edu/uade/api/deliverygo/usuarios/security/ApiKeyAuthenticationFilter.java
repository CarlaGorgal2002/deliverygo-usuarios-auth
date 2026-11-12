package ar.edu.uade.api.deliverygo.usuarios.security;

import ar.edu.uade.api.deliverygo.usuarios.exception.ErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

// Autentica a un módulo consumidor por su API Key (RN-15, CU-10). Sólo establece QUIÉN es el módulo;
// qué puede leer lo decide @PreAuthorize en cada controller. Si la solicitud trae Authorization, manda
// el JWT: un usuario no se hace pasar por un módulo ni al revés.
// No es un @Component: lo crea SecurityConfig para que corra una sola vez.
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-Api-Key";

    private final ModulosApiKeys modulos;
    private final ErrorResponseWriter errorWriter;

    public ApiKeyAuthenticationFilter(ModulosApiKeys modulos, ErrorResponseWriter errorWriter) {
        this.modulos = modulos;
        this.errorWriter = errorWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/") || RutasPublicas.esPublica(request);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clave = request.getHeader(API_KEY_HEADER);
        if (clave == null || request.getHeader(HttpHeaders.AUTHORIZATION) != null) {
            filterChain.doFilter(request, response);
            return;
        }

        Optional<String> modulo = modulos.identificar(clave);
        if (modulo.isEmpty()) {
            SecurityContextHolder.clearContext();
            errorWriter.escribir(request, response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED",
                    "La autenticación falta, es inválida o está vencida");
            return;
        }

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new ModuloAutenticado(modulo.get()), null, List.of(new SimpleGrantedAuthority("ROLE_MODULO"))));
        filterChain.doFilter(request, response);
    }
}
