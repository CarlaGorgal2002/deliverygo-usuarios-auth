package ar.edu.uade.api.deliverygo.usuarios.dto;

import ar.edu.uade.api.deliverygo.usuarios.domain.EstadoUsuario;
import io.swagger.v3.oas.annotations.media.Schema;

// DTO-15 (CU-10). Consulta liviana para otros módulos: sólo lo necesario para decidir si un usuario
// puede operar y con qué rol. No incluye datos personales.
public record AccesoUsuarioResponse(
        @Schema(example = "1") Long usuarioId,
        EstadoUsuario estado,
        @Schema(example = "CLIENTE", allowableValues = {"CLIENTE", "COMERCIO", "REPARTIDOR", "ADMIN"}) String rol
) {
}
