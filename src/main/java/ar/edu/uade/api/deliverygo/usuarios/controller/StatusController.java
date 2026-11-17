package ar.edu.uade.api.deliverygo.usuarios.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// Chequeo de vida del servicio: lo usan los otros módulos, el front o Docker para saber si Usuarios está levantado.
// Es público y no toca la base de datos.
@RestController
@Tag(name = "Estado", description = "Chequeo de que el servicio está levantado.")
public class StatusController {

    @Operation(operationId = "status", summary = "Estado del servicio",
            description = "Responde 200 con {\"status\":\"ok\"} si el servicio está levantado. Es público.")
    @ApiResponse(responseCode = "200", description = "El servicio está levantado.")
    @SecurityRequirements
    @GetMapping("/api/v1/status")
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}
