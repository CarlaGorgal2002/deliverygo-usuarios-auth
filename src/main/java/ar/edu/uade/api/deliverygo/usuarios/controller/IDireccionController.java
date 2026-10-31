package ar.edu.uade.api.deliverygo.usuarios.controller;

import ar.edu.uade.api.deliverygo.usuarios.dto.CrearDireccionRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.DireccionResponse;
import ar.edu.uade.api.deliverygo.usuarios.dto.ErrorResponse;
import ar.edu.uade.api.deliverygo.usuarios.dto.ModificarDireccionRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

// Contrato documentado de CU-05. Sólo el titular ACTIVO administra sus direcciones.
@Tag(name = "Direcciones", description = "CU-05. Listado, alta, modificación, baja lógica y predeterminada.")
public interface IDireccionController {

    String ID_POSITIVO = "Debe ser un entero positivo";

    String DIRECCION_NO_ENCONTRADA =
            "ADDRESS_NOT_FOUND: La dirección no existe, está dada de baja o pertenece a otro usuario (no se distingue la causa).";

    @Operation(operationId = "listarDirecciones", summary = "Listar direcciones (API-05 / CU-05)",
            description = "Devuelve las direcciones activas del titular, sin paginación. Sin direcciones devuelve 200 y []. Hay cero o una predeterminada.")
    @ApiResponse(responseCode = "200", description = "Direcciones activas del usuario.")
    @ErroresDeSesion
    @ErroresComunes
    ResponseEntity<List<DireccionResponse>> listarDirecciones(@PathVariable @Positive(message = ID_POSITIVO) Long id);

    @Operation(operationId = "crearDireccion", summary = "Crear dirección (API-06 / CU-05)",
            description = """
                    Crea una dirección activa del titular. predeterminada es false cuando se omite, incluso en la primera.
                    Si es true, desmarca la anterior y marca la nueva en una única transacción. El alta no es idempotente.""")
    @ApiResponse(responseCode = "201", description = "Dirección creada.",
            headers = @Header(name = "Location", description = "URI de la dirección creada.", schema = @Schema(example = "/api/v1/users/1/addresses/10")))
    @ErroresDeSesion
    @ErroresComunes
    ResponseEntity<DireccionResponse> crearDireccion(@PathVariable @Positive(message = ID_POSITIVO) Long id, @Valid @RequestBody CrearDireccionRequest request);

    @Operation(operationId = "modificarDireccion", summary = "Modificar dirección (API-07 / CU-05)",
            description = """
                    Reemplaza los datos editables de una dirección activa y propia (PUT completo). Omitir piso o departamento los elimina.
                    No modifica predeterminada: para eso existe API-09.""")
    @ApiResponse(responseCode = "200", description = "Dirección actualizada.")
    @ApiResponse(responseCode = "404", description = DIRECCION_NO_ENCONTRADA,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ErroresDeSesion
    @ErroresComunes
    ResponseEntity<DireccionResponse> modificarDireccion(@PathVariable @Positive(message = ID_POSITIVO) Long id, @PathVariable @Positive(message = ID_POSITIVO) Long addressId,
                                                         @Valid @RequestBody ModificarDireccionRequest request);

    @Operation(operationId = "eliminarDireccion", summary = "Eliminar dirección (API-08 / CU-05)",
            description = """
                    Baja lógica: la dirección queda inactiva para conservar referencias históricas. Si era la predeterminada, se desmarca
                    sin elegir otra. Sin cuerpo en la solicitud ni en la respuesta. Repetir el DELETE devuelve 404.""")
    @ApiResponse(responseCode = "204", description = "Dirección dada de baja; sin cuerpo.", content = @Content)
    @ApiResponse(responseCode = "404", description = DIRECCION_NO_ENCONTRADA,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ErroresDeSesion
    @ErroresComunes
    ResponseEntity<Void> eliminarDireccion(@PathVariable @Positive(message = ID_POSITIVO) Long id, @PathVariable @Positive(message = ID_POSITIVO) Long addressId);

    @Operation(operationId = "seleccionarDireccionPredeterminada", summary = "Seleccionar dirección predeterminada (API-09 / CU-05)",
            description = """
                    Sin cuerpo: addressId identifica la dirección a marcar. Desmarca la anterior y marca la elegida en una única
                    transacción. Si ya era la predeterminada, devuelve 200 con la misma dirección (idempotente).""")
    @ApiResponse(responseCode = "200", description = "Dirección predeterminada seleccionada.")
    @ApiResponse(responseCode = "404", description = DIRECCION_NO_ENCONTRADA,
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ErroresDeSesion
    @ErroresComunes
    ResponseEntity<DireccionResponse> seleccionarPredeterminada(@PathVariable @Positive(message = ID_POSITIVO) Long id, @PathVariable @Positive(message = ID_POSITIVO) Long addressId);
}
