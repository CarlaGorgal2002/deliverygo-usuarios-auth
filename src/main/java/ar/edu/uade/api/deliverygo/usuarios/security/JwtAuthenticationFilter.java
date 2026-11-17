package ar.edu.uade.api.deliverygo.usuarios.security;

import io.jsonwebtoken.JwtException;
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

// Autentica la solicitud a partir del Bearer token de una persona (RN-09). Sólo establece QUIÉN es el
// solicitante; qué puede hacer lo decide SecurityConfig (rol) y el controller (titular de la ruta, RN-05).
// Los módulos consumidores no mandan token (RN-15): sus lecturas llegan sin autenticar.
// Un token ausente, inválido o vencido no corta la solicitud: sigue sin autenticar y, si la ruta es
// protegida, SecurityConfig responde 401. Así las rutas públicas funcionan aunque el navegador mande un
// token viejo. No es un @Component: lo crea SecurityConfig para que corra una sola vez.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIJO_BEARER = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (cabecera != null && cabecera.regionMatches(true, 0, PREFIJO_BEARER, 0, PREFIJO_BEARER.length())) {
            try {
                UsuarioAutenticado usuario = jwtService.leer(cabecera.substring(PREFIJO_BEARER.length()).trim());
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                        usuario, null, List.of(new SimpleGrantedAuthority("ROLE_" + usuario.rol()))));
            } catch (JwtException | IllegalArgumentException e) {
                // Token inválido (o con un sujeto que no es un id de usuario): no se autentica a nadie.
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
