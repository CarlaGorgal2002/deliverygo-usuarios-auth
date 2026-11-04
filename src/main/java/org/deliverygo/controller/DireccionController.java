package org.deliverygo.controller;


import org.deliverygo.dto.CrearDireccionRequest;
import org.deliverygo.dto.DireccionResponse;
import org.deliverygo.dto.ModificarDireccionRequest;
import org.deliverygo.services.DireccionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/{id}/addresses")
public class DireccionController {
    private final DireccionService direccionService;

    public DireccionController(DireccionService direccionService) {
        this.direccionService = direccionService;
    }

    @GetMapping                                            // API-05
    public ResponseEntity<List<DireccionResponse>> listar(@PathVariable Long id) {
        return ResponseEntity.ok(direccionService.listarDirecciones(id));
    }

    @PostMapping                                           // API-06
    public ResponseEntity<DireccionResponse> crear(@PathVariable Long id,
                                                   @RequestBody CrearDireccionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(direccionService.crearDireccion(id, request));
    }

    @PutMapping("/{addressId}")                            // API-07
    public ResponseEntity<DireccionResponse> modificar(@PathVariable Long id,
                                                       @PathVariable Long addressId,
                                                       @RequestBody ModificarDireccionRequest request) {
        return ResponseEntity.ok(direccionService.modificarDireccion(id, addressId, request));
    }

    @DeleteMapping("/{addressId}")                         // API-08
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @PathVariable Long addressId) {
        direccionService.eliminarDireccion(id, addressId);
        return ResponseEntity.noContent().build();         // 204
    }

    @PatchMapping("/{addressId}/default")                  // API-09
    public ResponseEntity<DireccionResponse> seleccionarPredeterminada(@PathVariable Long id,
                                                                       @PathVariable Long addressId) {
        return ResponseEntity.ok(direccionService.seleccionarDireccionPredeterminada(id, addressId));
    }
}
