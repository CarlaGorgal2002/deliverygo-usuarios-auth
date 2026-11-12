package ar.edu.uade.api.deliverygo.usuarios.exception;

import ar.edu.uade.api.deliverygo.usuarios.dto.DetalleValidacion;
import ar.edu.uade.api.deliverygo.usuarios.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

// Arma el DTO-08 de forma idéntica para errores de controller, service y seguridad.
public final class ErrorResponses {

    // El filtro de la API guarda acá el X-Correlation-Id de la solicitud (DC-06).
    public static final String CORRELATION_ATTRIBUTE = "deliverygo.correlationId";
    public static final String WWW_AUTHENTICATE = "Bearer realm=\"deliverygo-usuarios\"";

    private ErrorResponses() {}

    public static ErrorResponse build(HttpServletRequest request, int status, String code, String message,
                                      List<DetalleValidacion> details) {
        Object correlationId = request.getAttribute(CORRELATION_ATTRIBUTE);
        return new ErrorResponse(
                OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS),
                status,
                code,
                message,
                request.getRequestURI(),
                correlationId != null ? correlationId.toString() : UUID.randomUUID().toString(),
                details == null || details.isEmpty() ? null : details);
    }
}
