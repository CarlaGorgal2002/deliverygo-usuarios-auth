package ar.edu.uade.api.deliverygo.usuarios.controller;

import ar.edu.uade.api.deliverygo.usuarios.dto.CrearDireccionRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.DireccionResponse;
import ar.edu.uade.api.deliverygo.usuarios.dto.ModificarDireccionRequest;
import ar.edu.uade.api.deliverygo.usuarios.service.DireccionService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

import static ar.edu.uade.api.deliverygo.usuarios.controller.UsuarioController.esModulo;
import static ar.edu.uade.api.deliverygo.usuarios.controller.UsuarioController.verificarTitular;

// RN-05: sólo el titular administra sus direcciones. Los módulos (Pedidos) las listan sin credencial (RN-15).
// La titularidad se verifica antes de consultar la dirección.
@RestController
@RequestMapping("/api/v1/users/{id}/addresses")
public class DireccionController implements IDireccionController {

    private final DireccionService direccionService;

    public DireccionController(DireccionService direccionService) {
        this.direccionService = direccionService;
    }

    @Override
    @GetMapping
    public ResponseEntity<List<DireccionResponse>> listarDirecciones(Long id, Authentication authentication) {
        if (esModulo(authentication)) {
            return ResponseEntity.ok(direccionService.listarDireccionesParaModulo(id));
        }
        verificarTitular(authentication, id);
        return ResponseEntity.ok(direccionService.listarDirecciones(id));
    }

    @Override
    @PostMapping
    public ResponseEntity<DireccionResponse> crearDireccion(Long id, CrearDireccionRequest request,
                                                            Authentication authentication) {
        verificarTitular(authentication, id);
        DireccionResponse creada = direccionService.crearDireccion(id, request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + id + "/addresses/" + creada.id())).body(creada);
    }

    @Override
    @PutMapping("/{addressId}")
    public ResponseEntity<DireccionResponse> modificarDireccion(Long id, Long addressId, ModificarDireccionRequest request,
                                                                Authentication authentication) {
        verificarTitular(authentication, id);
        return ResponseEntity.ok(direccionService.modificarDireccion(id, addressId, request));
    }

    @Override
    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> eliminarDireccion(Long id, Long addressId, Authentication authentication) {
        verificarTitular(authentication, id);
        direccionService.eliminarDireccion(id, addressId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/{addressId}/default")
    public ResponseEntity<DireccionResponse> seleccionarPredeterminada(Long id, Long addressId,
                                                                       Authentication authentication) {
        verificarTitular(authentication, id);
        return ResponseEntity.ok(direccionService.seleccionarPredeterminada(id, addressId));
    }
}
