package org.deliverygo.exceptions;

import org.deliverygo.dto.ErrorDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorDto> manejarIllegalArgument(IllegalArgumentException e) {

        ErrorDto error = new ErrorDto(HttpStatus.BAD_REQUEST.value(), e.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDto> manejarBodyInvalido(HttpMessageNotReadableException e) {

        ErrorDto error = new ErrorDto(HttpStatus.BAD_REQUEST.value(), "El cuerpo de la request está vacío o tiene un formato inválido");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }


}