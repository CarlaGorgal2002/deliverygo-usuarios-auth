package ar.edu.uade.api.deliverygo.usuarios.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

// Autentica a un módulo consumidor por su API Key (RN-15, CU-10) con el rol MODULO. Qué rutas puede usar
// lo decide SecurityConfig. Si la solicitud trae Authorization, manda el JWT: un usuario no se hace pasar
// por un módulo ni al revés. Una clave desconocida no autentica: si la ruta es protegida, SecurityConfig
// responde 401. No es un @Component: lo crea SecurityConfig para que corra una sola vez.
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-Api-Key";

    private final ModulosApiKeys modulos;

    public ApiKeyAuthenticationFilter(ModulosApiKeys modulos) {
        this.modulos = modulos;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clave = request.getHeader(API_KEY_HEADER);

        if (clave != null && request.getHeader(HttpHeaders.AUTHORIZATION) == null) {
            Optional<String> modulo = modulos.identificar(clave);
            if (modulo.isPresent()) {
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        new ModuloAutenticado(modulo.get()), null, List.of(new SimpleGrantedAuthority("ROLE_MODULO"))));
            }
        }

        filterChain.doFilter(request, response);
    }
}
