package org.deliverygo.controller;

import org.deliverygo.dto.ModificarPerfilRequest;
import org.deliverygo.dto.UsuarioResponse;
import org.deliverygo.services.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
public class UsuarioController {
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> consultarPerfil(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.consultarPerfil(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> modificarPerfil(@PathVariable Long id,
                                                           @RequestBody ModificarPerfilRequest request) {
        return ResponseEntity.ok(usuarioService.modificarPerfil(id, request));
    }
}
