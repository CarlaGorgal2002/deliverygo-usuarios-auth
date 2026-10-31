package ar.edu.uade.api.deliverygo.usuarios.controller;

import ar.edu.uade.api.deliverygo.usuarios.dto.ErrorResponse;
import ar.edu.uade.api.deliverygo.usuarios.dto.LoginRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.LoginResponse;
import ar.edu.uade.api.deliverygo.usuarios.dto.RegistrarUsuarioRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

// Contrato documentado de CU-01 y CU-02. Ambas operaciones son públicas.
@Tag(name = "Autenticación", description = "CU-01 y CU-02. Registro y emisión centralizada de JWT.")
public interface IAuthController {

    @Operation(operationId = "registrarUsuario", summary = "Registrar usuario (API-01 / CU-01)",
            description = """
                    Crea un usuario con rol CLIENTE, estado ACTIVO y email único. La contraseña se almacena como hash.
                    No emite JWT: después hay que iniciar sesión. La bienvenida (INT-01) es no bloqueante y hoy es un mock declarado.""")
    @SecurityRequirements
    @ApiResponse(responseCode = "201", description = "Usuario creado.",
            headers = @Header(name = "Location", description = "URI del usuario creado.", schema = @Schema(example = "/api/v1/users/1")))
    @ApiResponse(responseCode = "409", description = "EMAIL_ALREADY_REGISTERED: El email ya está registrado.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ErroresComunes
    ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistrarUsuarioRequest request);

    @Operation(operationId = "iniciarSesion", summary = "Iniciar sesión (API-02 / CU-02)",
            description = """
                    Autentica con email y password. Email inexistente y contraseña incorrecta dan el mismo 401.
                    El estado de la cuenta se informa recién después de verificar la contraseña: una cuenta INACTIVA devuelve 403.""")
    @SecurityRequirements
    @ApiResponse(responseCode = "200", description = "Sesión iniciada.")
    @ApiResponse(responseCode = "401", description = "INVALID_CREDENTIALS: Email o contraseña incorrectos.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "USER_INACTIVE: La cuenta está inactiva.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ErroresComunes
    ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request);
}
