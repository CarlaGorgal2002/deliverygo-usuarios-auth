package ar.edu.uade.api.deliverygo.usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;

// Detalle de un campo inválido dentro de DTO-08. No repite el valor recibido.
public record DetalleValidacion(
        @Schema(example = "email", description = "Nombre del campo, parámetro o cabecera; body si el JSON es ilegible.") String campo,
        @Schema(example = "Debe tener un formato de email válido") String mensaje
) {
}
