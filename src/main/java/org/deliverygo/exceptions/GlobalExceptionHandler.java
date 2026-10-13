package org.deliverygo.exceptions;

import org.deliverygo.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> manejarIllegalArgument(IllegalArgumentException e) {

        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), e.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarBodyInvalido(HttpMessageNotReadableException e) {

        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "El cuerpo de la request está vacío o tiene un formato inválido");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }


}