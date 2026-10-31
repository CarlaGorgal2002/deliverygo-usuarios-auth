package ar.edu.uade.api.deliverygo.usuarios.controller;

import ar.edu.uade.api.deliverygo.usuarios.dto.ErrorResponse;
import ar.edu.uade.api.deliverygo.usuarios.dto.ModificarPerfilRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.UsuarioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

// Contrato documentado de CU-03 y CU-04. Sólo el titular ACTIVO opera sobre su perfil.
@Tag(name = "Usuarios", description = "CU-03 y CU-04. Consulta y modificación del perfil propio.")
public interface IUsuarioController {

    String ID_POSITIVO = "Debe ser un entero positivo";

    @Operation(operationId = "consultarPerfil", summary = "Consultar perfil (API-03 / CU-03)",
            description = """
                    El titular consulta su propio perfil: el sub del JWT debe coincidir con el id y la cuenta debe estar ACTIVA.
                    No devuelve contraseña ni hash; las direcciones se obtienen en API-05.
                    El acceso de otros módulos con credencial propia está pendiente de acuerdo y no está habilitado.""")
    @ApiResponse(responseCode = "200", description = "Perfil del usuario.")
    @ErroresDeSesion
    @ErroresComunes
    ResponseEntity<UsuarioResponse> consultarPerfil(@PathVariable @Positive(message = ID_POSITIVO) Long id);

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
}
