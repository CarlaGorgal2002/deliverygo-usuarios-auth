package ar.edu.uade.api.deliverygo.usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import static ar.edu.uade.api.deliverygo.usuarios.dto.Validaciones.OBLIGATORIO;

// DTO-09. El PUT reemplaza los datos editables y conserva predeterminada; para cambiarla existe API-09.
public record ModificarDireccionRequest(
        @Schema(example = "Av. Independencia")
        @NotBlank(message = OBLIGATORIO) @Size(max = 100, message = "No puede superar 100 caracteres")
        String calle,

        @Schema(example = "1200")
        @NotBlank(message = OBLIGATORIO) @Size(max = 10, message = "No puede superar 10 caracteres")
        String numero,

        @Schema(example = "2", nullable = true, description = "Omitir o null elimina el valor anterior.")
        @Size(max = 10, message = "No puede superar 10 caracteres")
        String piso,

        @Schema(example = "B", nullable = true, description = "Omitir o null elimina el valor anterior.")
        @Size(max = 10, message = "No puede superar 10 caracteres")
        String departamento,

        @Schema(example = "Ciudad Autónoma de Buenos Aires")
        @NotBlank(message = OBLIGATORIO) @Size(max = 80, message = "No puede superar 80 caracteres")
        String ciudad,

        @Schema(example = "Ciudad Autónoma de Buenos Aires")
        @NotBlank(message = OBLIGATORIO) @Size(max = 80, message = "No puede superar 80 caracteres")
        String provincia,

        @Schema(example = "C1099AAZ")
        @NotBlank(message = OBLIGATORIO) @Size(max = 10, message = "No puede superar 10 caracteres")
        String codigoPostal
) {
}
