package ar.edu.uade.api.deliverygo.usuarios.controller;

import ar.edu.uade.api.deliverygo.usuarios.dto.AccesoUsuarioResponse;
import ar.edu.uade.api.deliverygo.usuarios.dto.CambiarEstadoRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.CambiarRolRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.ErrorResponse;
import ar.edu.uade.api.deliverygo.usuarios.dto.ModificarPerfilRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.UsuarioResponse;
import ar.edu.uade.api.deliverygo.usuarios.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

// Contrato documentado de CU-03, CU-04, CU-06, CU-07 y CU-10.
@Tag(name = "Usuarios", description = "CU-03, CU-04, CU-06, CU-07 y CU-10. Perfil propio, administración de cuentas y consulta para otros módulos.")
public interface IUsuarioController {

    String ID_POSITIVO = "Debe ser un entero positivo";

    @Operation(operationId = "consultarPerfil", summary = "Consultar perfil (API-03 / CU-03)",
            description = """
                    Con JWT: el titular consulta su propio perfil (el sub debe coincidir con el id) y la cuenta debe estar ACTIVA.
                    Con X-Api-Key: cualquier módulo consumidor autorizado consulta el perfil de un usuario y recibe 200 también si la cuenta
                    está INACTIVA, para decidir con sus propias reglas. Un JWT de otra persona, aunque sea ADMIN, recibe 403.
                    No devuelve contraseña ni hash; las direcciones se obtienen en API-05.""")
    @SecurityRequirements({@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "moduleApiKey")})
    @ApiResponse(responseCode = "200", description = "Perfil del usuario.")
    @ErroresDeSesion
    @ErroresComunes
    ResponseEntity<UsuarioResponse> consultarPerfil(@PathVariable @Positive(message = ID_POSITIVO) Long id,
                                                    @Parameter(hidden = true) Authentication authentication);

    @Operation(operationId = "modificarPerfil", summary = "Modificar perfil (API-04 / CU-04)",
            description = """
                    Reemplaza nombre, apellido, email y telefono (PUT completo). Omitir telefono, enviarlo null o en blanco lo elimina.
                    No admite rol, estado ni contraseña. Cambiar el email no cambia el sub del JWT.""")
    @ApiResponse(responseCode = "200", description = "Perfil actualizado.")
    @ApiResponse(responseCode = "409", description = "EMAIL_ALREADY_REGISTERED: El email pertenece a otro usuario.",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ErroresDeSesion
    @ErroresComunes
    ResponseEntity<UsuarioResponse> modificarPerfil(@PathVariable @Positive(message = ID_POSITIVO) Long id, @Valid @RequestBody ModificarPerfilRequest request);

    @Operation(operationId = "cambiarEstadoUsuario", summary = "Desactivar o reactivar usuario (API-10 / CU-06)",
            description = """
                    Sólo un ADMIN ACTIVO (se verifica contra la base, no sólo con el rol del token). Deja la cuenta en el estado pedido;
                    repetir el estado actual es idempotente. Un administrador no puede cambiar su propia cuenta.
                    Una cuenta INACTIVA no puede iniciar sesión ni operar; sus JWT ya emitidos dejan de servir en las operaciones
                    sobre su perfil y direcciones, y API-15 los rechaza.""")
    @ApiResponse(responseCode = "200", description = "Cuenta con el estado pedido.")
    @ErroresDeSesion
    @ErroresComunes
    ResponseEntity<UsuarioResponse> cambiarEstado(@PathVariable @Positive(message = ID_POSITIVO) Long id,
                                                  @Valid @RequestBody CambiarEstadoRequest request,
                                                  @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado administrador);

    @Operation(operationId = "cambiarRolUsuario", summary = "Asignar o modificar rol (API-11 / CU-07)",
            description = """
                    Sólo un ADMIN ACTIVO. Asigna uno de los roles del catálogo (CLIENTE, COMERCIO, REPARTIDOR, ADMIN); repetir el rol actual es
                    idempotente. Un administrador no puede cambiar su propio rol.
                    Los JWT ya emitidos conservan el rol anterior hasta vencer: para conocer el rol vigente usar API-15 o API-16.""")
    @ApiResponse(responseCode = "200", description = "Cuenta con el rol asignado.")
    @ErroresDeSesion
    @ErroresComunes
    ResponseEntity<UsuarioResponse> cambiarRol(@PathVariable @Positive(message = ID_POSITIVO) Long id,
                                               @Valid @RequestBody CambiarRolRequest request,
                                               @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado administrador);

    @Operation(operationId = "consultarAccesoUsuario", summary = "Consultar estado y rol para otro módulo (API-16 / CU-10)",
            description = """
                    Consulta liviana para módulos consumidores: devuelve sólo el estado y el rol vigentes, sin datos personales.
                    Se autentica con la API Key del módulo en X-Api-Key; un JWT de usuario recibe 403. Una cuenta INACTIVA se devuelve con 200.""")
    @SecurityRequirements(@SecurityRequirement(name = "moduleApiKey"))
    @ApiResponse(responseCode = "200", description = "Estado y rol del usuario.")
    @ErroresDeSesion
    @ErroresComunes
    ResponseEntity<AccesoUsuarioResponse> consultarAcceso(@PathVariable @Positive(message = ID_POSITIVO) Long id);
}
