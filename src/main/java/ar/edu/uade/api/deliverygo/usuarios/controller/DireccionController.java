package ar.edu.uade.api.deliverygo.usuarios.controller;

import ar.edu.uade.api.deliverygo.usuarios.dto.CrearDireccionRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.DireccionResponse;
import ar.edu.uade.api.deliverygo.usuarios.dto.ModificarDireccionRequest;
import ar.edu.uade.api.deliverygo.usuarios.service.DireccionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

// RN-05: ver UsuarioController. La titularidad se evalúa antes de consultar la dirección.
@RestController
@RequestMapping("/api/v1/users/{id}/addresses")
public class DireccionController implements IDireccionController {

    private final DireccionService direccionService;

    public DireccionController(DireccionService direccionService) {
        this.direccionService = direccionService;
    }

    @Override
    @GetMapping
    @PreAuthorize("principal.id() == #id")
    public ResponseEntity<List<DireccionResponse>> listarDirecciones(Long id) {
        return ResponseEntity.ok(direccionService.listarDirecciones(id));
    }

    @Override
    @PostMapping
    @PreAuthorize("principal.id() == #id")
    public ResponseEntity<DireccionResponse> crearDireccion(Long id, CrearDireccionRequest request) {
        DireccionResponse creada = direccionService.crearDireccion(id, request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + id + "/addresses/" + creada.id())).body(creada);
    }

    @Override
    @PutMapping("/{addressId}")
    @PreAuthorize("principal.id() == #id")
    public ResponseEntity<DireccionResponse> modificarDireccion(Long id, Long addressId, ModificarDireccionRequest request) {
        return ResponseEntity.ok(direccionService.modificarDireccion(id, addressId, request));
    }

    @Override
    @DeleteMapping("/{addressId}")
    @PreAuthorize("principal.id() == #id")
    public ResponseEntity<Void> eliminarDireccion(Long id, Long addressId) {
        direccionService.eliminarDireccion(id, addressId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/{addressId}/default")
    @PreAuthorize("principal.id() == #id")
    public ResponseEntity<DireccionResponse> seleccionarPredeterminada(Long id, Long addressId) {
        return ResponseEntity.ok(direccionService.seleccionarPredeterminada(id, addressId));
    }
}
