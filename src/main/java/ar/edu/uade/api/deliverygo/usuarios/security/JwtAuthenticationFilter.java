package ar.edu.uade.api.deliverygo.usuarios.security;

import ar.edu.uade.api.deliverygo.usuarios.exception.ErrorResponseWriter;
import io.jsonwebtoken.JwtException;
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

// Autentica la solicitud a partir del Bearer token (RN-09). Sólo establece QUIÉN es el solicitante;
// que sea el titular de la ruta (RN-05) lo decide @PreAuthorize en cada controller.
// No es un @Component: lo crea SecurityConfig para que corra una sola vez, dentro de la cadena de seguridad.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtService jwtService;
    private final ErrorResponseWriter errorWriter;

    public JwtAuthenticationFilter(JwtService jwtService, ErrorResponseWriter errorWriter) {
        this.jwtService = jwtService;
        this.errorWriter = errorWriter;
    }

    // Registro y login son públicos: un token viejo guardado en el navegador no debe romperlos.
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/api/") || path.startsWith("/api/v1/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Sin token: sigue la cadena; si el endpoint exige autenticación responde el entry point (401).
        if (cabecera == null || !cabecera.regionMatches(true, 0, PREFIJO_BEARER, 0, PREFIJO_BEARER.length())) {
            filterChain.doFilter(request, response);
            return;
        }

        UsuarioAutenticado usuario;
        try {
            usuario = jwtService.leer(cabecera.substring(PREFIJO_BEARER.length()).trim());
        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            errorWriter.escribir(request, response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED",
                    "La autenticación falta, es inválida o está vencida");
            return;
        }

        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                usuario, null, List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol()))));

        filterChain.doFilter(request, response);
    }
}
