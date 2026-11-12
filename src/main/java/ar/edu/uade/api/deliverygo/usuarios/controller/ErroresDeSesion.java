package ar.edu.uade.api.deliverygo.usuarios.controller;

import ar.edu.uade.api.deliverygo.usuarios.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// Respuestas de las operaciones protegidas por Bearer token y por titularidad de la ruta (RN-04, RN-05, RN-09).
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "UNAUTHENTICATED: La autenticación falta, es inválida o está vencida.",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "403",
                description = "FORBIDDEN: No tiene permiso para acceder a este recurso; USER_INACTIVE: La cuenta está inactiva.",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "USER_NOT_FOUND: Usuario no encontrado.",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
})
public @interface ErroresDeSesion {
}
