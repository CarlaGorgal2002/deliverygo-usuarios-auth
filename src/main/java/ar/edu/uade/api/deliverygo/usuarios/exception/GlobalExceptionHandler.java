package ar.edu.uade.api.deliverygo.usuarios.exception;

import ar.edu.uade.api.deliverygo.usuarios.dto.DetalleValidacion;
import ar.edu.uade.api.deliverygo.usuarios.dto.ErrorResponse;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

// Único punto donde las excepciones se convierten en DTO-08. Extiende ResponseEntityExceptionHandler
// para que los errores propios de Spring MVC (405, 415, ruta inexistente...) usen el mismo formato.
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String MSG_VALIDACION = "La solicitud contiene datos inválidos";
    private static final String MSG_PROHIBIDO = "No tiene permiso para acceder a este recurso";

    // ---------------------------------------------------------------- reglas de negocio

    // 400 - RN-01 (lo que el DTO no alcanza a validar)
    @ExceptionHandler(DatoInvalidoException.class)
    public ResponseEntity<Object> manejarDatoInvalido(DatoInvalidoException e, HttpServletRequest request) {
        return responder(request, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", MSG_VALIDACION,
                List.of(new DetalleValidacion(e.getCampo(), e.getMessage())));
    }

    // 409 - RN-02
    @ExceptionHandler(EmailDuplicadoException.class)
    public ResponseEntity<Object> manejarEmailDuplicado(EmailDuplicadoException e, HttpServletRequest request) {
        return responder(request, HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", e.getMessage(), null);
    }

    // 401 - RN-08: mismo error para email inexistente y contraseña incorrecta
    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<Object> manejarCredenciales(CredencialesInvalidasException e, HttpServletRequest request) {
        return responder(request, HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", e.getMessage(), null);
    }

    // 403 - RN-04
    @ExceptionHandler(UsuarioInactivoException.class)
    public ResponseEntity<Object> manejarUsuarioInactivo(UsuarioInactivoException e, HttpServletRequest request) {
        return responder(request, HttpStatus.FORBIDDEN, "USER_INACTIVE", e.getMessage(), null);
    }

    // 403 - RN-05: lo lanza @PreAuthorize cuando el token no pertenece al titular de la ruta
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> manejarAccesoDenegado(AccessDeniedException e, HttpServletRequest request) {
        return responder(request, HttpStatus.FORBIDDEN, "FORBIDDEN", MSG_PROHIBIDO, null);
    }

    // 403 - RN-11 y RN-12: se pidió una operación que el solicitante autenticado no puede realizar
    @ExceptionHandler(AccesoProhibidoException.class)
    public ResponseEntity<Object> manejarAccesoProhibido(AccesoProhibidoException e, HttpServletRequest request) {
        return responder(request, HttpStatus.FORBIDDEN, "FORBIDDEN", e.getMessage(), null);
    }

    // 401 - RN-09: el token es correcto pero la sesión que representa ya no sirve
    @ExceptionHandler(SesionInvalidaException.class)
    public ResponseEntity<Object> manejarSesionInvalida(SesionInvalidaException e, HttpServletRequest request) {
        return responder(request, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", e.getMessage(), null);
    }

    // 400 - RN-13: token de recuperación inexistente, vencido o usado
    @ExceptionHandler(TokenRecuperacionInvalidoException.class)
    public ResponseEntity<Object> manejarTokenRecuperacion(TokenRecuperacionInvalidoException e, HttpServletRequest request) {
        return responder(request, HttpStatus.BAD_REQUEST, "INVALID_RESET_TOKEN", e.getMessage(), null);
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<Object> manejarUsuarioNoEncontrado(UsuarioNoEncontradoException e, HttpServletRequest request) {
        return responder(request, HttpStatus.NOT_FOUND, "USER_NOT_FOUND", e.getMessage(), null);
    }

    @ExceptionHandler(DireccionNoEncontradaException.class)
    public ResponseEntity<Object> manejarDireccionNoEncontrada(DireccionNoEncontradaException e, HttpServletRequest request) {
        return responder(request, HttpStatus.NOT_FOUND, "ADDRESS_NOT_FOUND", e.getMessage(), null);
    }

    // 500 - nunca se expone el detalle; queda en el log con el correlationId
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> manejarGenerico(Exception e, HttpServletRequest request) {
        log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), e);
        return responder(request, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Ocurrió un error interno", null);
    }

    // ---------------------------------------------------------------- validación de la entrada (400)

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        List<DetalleValidacion> detalles = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            detalles.add(new DetalleValidacion(error.getField(), error.getDefaultMessage()));
        }
        for (ObjectError error : ex.getBindingResult().getGlobalErrors()) {
            detalles.add(new DetalleValidacion("body", error.getDefaultMessage()));
        }
        return validacion(request, detalles);
    }

    // Se usa cuando algún parámetro de ruta lleva restricciones (por ejemplo @Positive en el id).
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                           HttpHeaders headers, HttpStatusCode status,
                                                                           WebRequest request) {
        List<DetalleValidacion> detalles = new ArrayList<>();
        for (ParameterValidationResult resultado : ex.getParameterValidationResults()) {
            if (resultado instanceof ParameterErrors errores) {
                errores.getFieldErrors().forEach(e -> detalles.add(new DetalleValidacion(e.getField(), e.getDefaultMessage())));
                errores.getGlobalErrors().forEach(e -> detalles.add(new DetalleValidacion("body", e.getDefaultMessage())));
            } else {
                String campo = resultado.getMethodParameter().getParameterName();
                for (MessageSourceResolvable error : resultado.getResolvableErrors()) {
                    detalles.add(new DetalleValidacion(campo != null ? campo : "parametro", error.getDefaultMessage()));
                }
            }
        }
        return validacion(request, detalles);
    }

    // JSON ilegible, campo desconocido (DC-01) o de tipo incorrecto.
    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        DetalleValidacion detalle;
        Throwable causa = ex.getCause();
        if (causa instanceof UnrecognizedPropertyException desconocida) {
            detalle = new DetalleValidacion(desconocida.getPropertyName(), "Campo no permitido");
        } else if (causa instanceof MismatchedInputException tipo && !tipo.getPath().isEmpty()) {
            String campo = tipo.getPath().stream()
                    .map(r -> r.getFieldName() != null ? r.getFieldName() : "[" + r.getIndex() + "]")
                    .collect(Collectors.joining("."));
            detalle = new DetalleValidacion(campo, "Tipo de dato inválido");
        } else {
            detalle = new DetalleValidacion("body", "El cuerpo debe ser JSON válido");
        }
        return validacion(request, List.of(detalle));
    }

    // Identificadores no numéricos o fuera del rango de Long (DC-07).
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatusCode status, WebRequest request) {
        String campo = ex instanceof MethodArgumentTypeMismatchException m ? m.getName() : ex.getPropertyName();
        return validacion(request, List.of(new DetalleValidacion(campo != null ? campo : "parametro", "Tiene un formato inválido")));
    }

    // ---------------------------------------------------------------- resto de errores de Spring MVC

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();
        int status = statusCode.value();
        if (status >= 500) {
            log.error("Error de infraestructura web en {} {}", servletRequest.getMethod(), servletRequest.getRequestURI(), ex);
        }
        // Estos códigos técnicos no pertenecen a una operación del contrato (no hay ruta, método o formato).
        String code = switch (status) {
            case 400 -> "VALIDATION_ERROR";
            case 404 -> "NOT_FOUND";
            case 405 -> "METHOD_NOT_ALLOWED";
            case 406 -> "NOT_ACCEPTABLE";
            case 415 -> "UNSUPPORTED_MEDIA_TYPE";
            default -> status >= 500 ? "INTERNAL_ERROR" : "REQUEST_ERROR";
        };
        String message = switch (status) {
            case 400 -> MSG_VALIDACION;
            case 404 -> "Recurso no encontrado";
            case 405 -> "Método HTTP no permitido";
            case 406 -> "Formato de respuesta no soportado";
            case 415 -> "Tipo de contenido no soportado";
            default -> status >= 500 ? "Ocurrió un error interno" : "La solicitud no pudo procesarse";
        };
        return responder(servletRequest, statusCode, code, message, null, headers);
    }

    // ---------------------------------------------------------------- utilidades

    private ResponseEntity<Object> validacion(WebRequest request, List<DetalleValidacion> detalles) {
        List<DetalleValidacion> ordenados = detalles.stream()
                .sorted(Comparator.comparing(DetalleValidacion::campo, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(DetalleValidacion::mensaje, Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
        return responder(((ServletWebRequest) request).getRequest(), HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR", MSG_VALIDACION, ordenados);
    }

    private ResponseEntity<Object> responder(HttpServletRequest request, HttpStatusCode status, String code,
                                             String message, List<DetalleValidacion> detalles) {
        return responder(request, status, code, message, detalles, new HttpHeaders());
    }

    private ResponseEntity<Object> responder(HttpServletRequest request, HttpStatusCode status, String code,
                                             String message, List<DetalleValidacion> detalles, HttpHeaders headers) {
        HttpHeaders respuesta = new HttpHeaders();
        if (headers != null) {
            respuesta.putAll(headers);
        }
        if (status.value() == HttpStatus.UNAUTHORIZED.value()) {
            respuesta.set(HttpHeaders.WWW_AUTHENTICATE, ErrorResponses.WWW_AUTHENTICATE);
        }
        ErrorResponse error = ErrorResponses.build(request, status.value(), code, message, detalles);
        return ResponseEntity.status(status).headers(respuesta).body(error);
    }
}
