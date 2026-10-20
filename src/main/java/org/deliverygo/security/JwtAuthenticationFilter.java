package org.deliverygo.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // /api/v1/users/{id} y todo lo que cuelga de ahí
    private static final Pattern RUTA_USUARIO = Pattern.compile("^/api/v1/users/(\\d{1,18})(/.*)?$");

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    // Registro, login y documentación no pasan por este filtro:
    // un token viejo en el navegador no tiene que romper el login.
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/auth/")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/h2-console");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        // Sin token: sigue. Spring decide si el endpoint lo exige.
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);

        String email;
        String rol;
        Long uid;

        try {
            email = jwtService.obtenerEmail(token);
            rol = jwtService.obtenerRol(token);
            uid = jwtService.obtenerUserId(token);

            if (email == null || rol == null || uid == null) {
                throw new IllegalArgumentException("Token incompleto");
            }
        } catch (JwtException | IllegalArgumentException ex) {
            // RN-09: token inválido o vencido
            SecurityContextHolder.clearContext();
            escribirError(request, response, 401, "SESION_INVALIDA", "Sesión inválida o vencida");
            return;
        }

        // RN-05: identidad del solicitante contra el {id} de la ruta
        Matcher m = RUTA_USUARIO.matcher(request.getRequestURI());
        if (m.matches()) {
            Long idRuta = Long.valueOf(m.group(1));
            boolean esTitular = uid.equals(idRuta);
            boolean esLectura = "GET".equals(request.getMethod());
            boolean esModulo = !"CLIENTE".equals(rol);   // provisorio, ver nota

            if (!esTitular && !(esLectura && esModulo)) {
                escribirError(request, response, 403, "ACCESO_DENEGADO",
                        "No tiene permiso para acceder a este recurso");
                return;
            }
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            email,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
            authentication.setDetails(uid);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    // Mismo formato que ErrorResponse (DTO-08)
    private void escribirError(HttpServletRequest request, HttpServletResponse response,
                               int status, String code, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String json = "{\"timestamp\":\"" + LocalDateTime.now() + "\","
                + "\"status\":" + status + ","
                + "\"code\":\"" + code + "\","
                + "\"message\":\"" + message + "\","
                + "\"path\":\"" + request.getRequestURI() + "\"}";
        response.getWriter().write(json);
    }
}