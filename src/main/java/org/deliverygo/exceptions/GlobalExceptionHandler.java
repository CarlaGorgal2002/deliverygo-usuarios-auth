package org.deliverygo.exceptions;

import org.deliverygo.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private ResponseEntity<ErrorResponse> responder(HttpStatus status, String code, String mensaje) {
        return ResponseEntity.status(status).body(new ErrorResponse(status.value(), code, mensaje));
    }

    // 400 - RN-01, RN-10
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> manejarIllegalArgument(IllegalArgumentException e) {
        return responder(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS", e.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarBodyInvalido(HttpMessageNotReadableException e) {
        return responder(HttpStatus.BAD_REQUEST, "CUERPO_INVALIDO",
                "El cuerpo de la request está vacío o tiene un formato inválido");
    }

    // 401 - RN-08 (mensaje genérico)
    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ErrorResponse> manejarCredenciales(CredencialesInvalidasException e) {
        return responder(HttpStatus.UNAUTHORIZED, "CREDENCIALES_INVALIDAS", e.getMessage());
    }

    // 403 - RN-05
    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ErrorResponse> manejarAccesoDenegado(AccesoDenegadoException e) {
        return responder(HttpStatus.FORBIDDEN, "ACCESO_DENEGADO", e.getMessage());
    }

    // 404
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorResponse> manejarNoEncontrado(RecursoNoEncontradoException e) {
        return responder(HttpStatus.NOT_FOUND, "RECURSO_NO_ENCONTRADO", e.getMessage());
    }

    // 409 - RN-02
    @ExceptionHandler(EmailDuplicadoException.class)
    public ResponseEntity<ErrorResponse> manejarEmailDuplicado(EmailDuplicadoException e) {
        return responder(HttpStatus.CONFLICT, "EMAIL_DUPLICADO", e.getMessage());
    }

    // 422 - RN-04
    @ExceptionHandler(UsuarioInactivoException.class)
    public ResponseEntity<ErrorResponse> manejarUsuarioInactivo(UsuarioInactivoException e) {
        return responder(HttpStatus.UNPROCESSABLE_ENTITY, "USUARIO_INACTIVO", e.getMessage());
    }

    // 500 - error no controlado (no exponer el detalle)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> manejarGenerico(Exception e) {
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "ERROR_INTERNO", "Error interno del servidor");
    }
    /**
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> manejarIllegalArgument(IllegalArgumentException e) {

        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(),"DATO INVALIDO", e.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarBodyInvalido(HttpMessageNotReadableException e) {

        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(),"CUERPO INVALIDO","El cuerpo de la request está vacío o tiene un formato inválido");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(EmailDuplicadoException.class)
    public ResponseEntity<ErrorResponse> manejarEmailDuplicado(EmailDuplicadoException e) {
        ErrorResponse error = new ErrorResponse(HttpStatus.CONFLICT.value(),"EMAIL DUPLICADO", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
**/

}