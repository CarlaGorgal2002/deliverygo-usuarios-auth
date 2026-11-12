package ar.edu.uade.api.deliverygo.usuarios.exception;

import ar.edu.uade.api.deliverygo.usuarios.dto.DetalleValidacion;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

// Escribe el DTO-08 directamente en la respuesta. Lo usan los componentes que actúan antes de que
// exista un controller (filtros, entry point y access denied handler de Spring Security).
@Component
public class ErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public ErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void escribir(HttpServletRequest request, HttpServletResponse response,
                         HttpStatus status, String code, String message) throws IOException {
        escribir(request, response, status, code, message, null);
    }

    public void escribir(HttpServletRequest request, HttpServletResponse response,
                         HttpStatus status, String code, String message,
                         List<DetalleValidacion> details) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        if (status == HttpStatus.UNAUTHORIZED) {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, ErrorResponses.WWW_AUTHENTICATE);
        }
        objectMapper.writeValue(response.getWriter(),
                ErrorResponses.build(request, status.value(), code, message, details));
    }
}
