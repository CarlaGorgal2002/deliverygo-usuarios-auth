package ar.edu.uade.api.deliverygo.usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import static ar.edu.uade.api.deliverygo.usuarios.dto.Validaciones.OBLIGATORIO;

// DTO-11 (CU-07). El rol debe ser uno del catálogo.
public record CambiarRolRequest(
        @Schema(example = "COMERCIO", allowableValues = {"CLIENTE", "COMERCIO", "REPARTIDOR", "ADMIN"})
        @NotBlank(message = OBLIGATORIO)
        @Pattern(regexp = "CLIENTE|COMERCIO|REPARTIDOR|ADMIN", message = "Debe ser CLIENTE, COMERCIO, REPARTIDOR o ADMIN")
        String rol
) {
}
