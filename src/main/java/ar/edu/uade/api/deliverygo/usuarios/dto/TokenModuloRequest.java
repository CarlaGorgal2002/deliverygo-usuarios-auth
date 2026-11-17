package ar.edu.uade.api.deliverygo.usuarios.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import static ar.edu.uade.api.deliverygo.usuarios.dto.Validaciones.OBLIGATORIO;

// DTO-16. Credencial con la que un módulo consumidor pide su token a Usuarios (RN-15).
public record TokenModuloRequest(
        @Schema(example = "pedidos", description = "Nombre del módulo: pedidos, logistica, catalogo, notificaciones, pagos u opiniones-analitica.")
        @NotBlank(message = OBLIGATORIO)
        String modulo,

        @Schema(example = "pedidos-demo-local-2026", accessMode = Schema.AccessMode.WRITE_ONLY,
                description = "Clave que el equipo de Usuarios entregó al módulo (variable MODULE_KEY_*).")
        @NotBlank(message = OBLIGATORIO)
        String clave
) {
}
