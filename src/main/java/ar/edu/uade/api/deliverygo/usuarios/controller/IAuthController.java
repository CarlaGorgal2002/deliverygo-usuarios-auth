package ar.edu.uade.api.deliverygo.usuarios.controller;

import ar.edu.uade.api.deliverygo.usuarios.dto.ErrorResponse;
import ar.edu.uade.api.deliverygo.usuarios.dto.LoginRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.LoginResponse;
import ar.edu.uade.api.deliverygo.usuarios.dto.RegistrarUsuarioRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.RestablecerPasswordRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.SesionResponse;
import ar.edu.uade.api.deliverygo.usuarios.dto.SolicitarRecuperacionRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.UsuarioResponse;
import ar.edu.uade.api.deliverygo.usuarios.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

// Contrato documentado de CU-01, CU-02, CU-08 y CU-09. Registro, login y recuperación son públicos;
// renovar y validar la sesión exigen un JWT.
@Tag(name = "Autenticación", description = "CU-01, CU-02, CU-08 y CU-09. Registro, emisión y renovación de JWT, y recuperación de contraseña.")
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

    @Operation(operationId = "solicitarRecuperacionPassword", summary = "Solicitar recuperación de contraseña (API-12 / CU-08)",
            description = """
                    Pide a Notificaciones que envíe un token de un solo uso (vigencia 30 minutos por defecto) al email de la cuenta.
                    Responde siempre 202, exista o no la cuenta y esté o no activa, para no revelar qué emails están registrados (RN-13).
                    Un pedido nuevo deja sin efecto el token anterior. El envío es el mock declarado INT-06.""")
    @SecurityRequirements
    @ApiResponse(responseCode = "202", description = "Solicitud recibida; sin cuerpo. Es igual para cuentas existentes e inexistentes.")
    @ErroresComunes
    ResponseEntity<Void> solicitarRecuperacion(@Valid @RequestBody SolicitarRecuperacionRequest request);

    @Operation(operationId = "restablecerPassword", summary = "Restablecer contraseña (API-13 / CU-08)",
            description = """
                    Cambia la contraseña con el token recibido por Notificaciones. El token se consume una sola vez.
                    Token inexistente, vencido, ya usado o de una cuenta inactiva dan el mismo 400 INVALID_RESET_TOKEN.
                    No inicia sesión: después hay que hacer login. Los JWT ya emitidos siguen vigentes hasta que venzan (DM-03).""")
    @SecurityRequirements
    @ApiResponse(responseCode = "204", description = "Contraseña actualizada; sin cuerpo.")
    @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR: Datos inválidos. INVALID_RESET_TOKEN: El token es inválido, venció o ya fue usado.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "INTERNAL_ERROR: Ocurrió un error interno.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    ResponseEntity<Void> restablecerPassword(@Valid @RequestBody RestablecerPasswordRequest request);

    @Operation(operationId = "renovarSesion", summary = "Renovar sesión (API-14 / CU-09)",
            description = """
                    Con un JWT todavía vigente emite uno nuevo, con el rol actual de la cuenta. Un token vencido no se renueva: hay que iniciar sesión.
                    La cuenta debe seguir ACTIVA. No admite API Key de módulo.""")
    @ApiResponse(responseCode = "200", description = "Sesión renovada.")
    @ApiResponse(responseCode = "401", description = "UNAUTHENTICATED: El token falta, es inválido o está vencido, o la cuenta ya no existe.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "USER_INACTIVE: La cuenta está inactiva. FORBIDDEN: La credencial no es de una persona.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "INTERNAL_ERROR: Ocurrió un error interno.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    ResponseEntity<LoginResponse> renovarSesion(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado actual);

    @Operation(operationId = "validarSesion", summary = "Validar sesión (API-15 / CU-09)",
            description = """
                    Valida el JWT presentado contra la base: firma, vencimiento, existencia y estado de la cuenta.
                    Devuelve el rol y el estado vigentes, que pueden diferir de los del token si la cuenta cambió después de emitirlo.""")
    @ApiResponse(responseCode = "200", description = "Sesión válida.")
    @ApiResponse(responseCode = "401", description = "UNAUTHENTICATED: El token falta, es inválido o está vencido, o la cuenta ya no existe.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "USER_INACTIVE: La cuenta está inactiva. FORBIDDEN: La credencial no es de una persona.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "500", description = "INTERNAL_ERROR: Ocurrió un error interno.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    ResponseEntity<SesionResponse> validarSesion(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado actual);
}
