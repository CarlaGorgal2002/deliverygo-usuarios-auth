package ar.edu.uade.api.deliverygo.usuarios.security;

import ar.edu.uade.api.deliverygo.usuarios.dto.DetalleValidacion;
import ar.edu.uade.api.deliverygo.usuarios.exception.ErrorResponseWriter;
import ar.edu.uade.api.deliverygo.usuarios.exception.ErrorResponses;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

// Primer filtro de cada solicitud a /api: fija el X-Correlation-Id (DC-06) y evita que las respuestas
// de la API se guarden en cachés compartidas. Corre antes que Spring Security para que hasta un
// 401 salga con correlación.
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiRequestFilter extends OncePerRequestFilter {

    public static final String CORRELATION_HEADER = "X-Correlation-Id";

    private static final Pattern CORRELATION_VALIDO = Pattern.compile("^[A-Za-z0-9._-]{1,128}$");

    private final ErrorResponseWriter errorWriter;

    public ApiRequestFilter(ErrorResponseWriter errorWriter) {
        this.errorWriter = errorWriter;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String recibido = request.getHeader(CORRELATION_HEADER);
        boolean invalido = recibido != null && !CORRELATION_VALIDO.matcher(recibido).matches();
        // Si la cabecera es inválida no se reutiliza: se genera un ID nuevo y se rechaza la solicitud.
        String correlationId = recibido == null || invalido ? UUID.randomUUID().toString() : recibido;

        request.setAttribute(ErrorResponses.CORRELATION_ATTRIBUTE, correlationId);
        response.setHeader(CORRELATION_HEADER, correlationId);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        MDC.put("correlationId", correlationId);
        try {
            if (invalido) {
                errorWriter.escribir(request, response, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                        "La solicitud contiene datos inválidos",
                        List.of(new DetalleValidacion(CORRELATION_HEADER,
                                "Debe tener entre 1 y 128 caracteres: letras, números, punto, guion o guion bajo")));
                return;
            }
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
        }
    }
}
