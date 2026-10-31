package ar.edu.uade.api.deliverygo.usuarios.controller;

import ar.edu.uade.api.deliverygo.usuarios.dto.ModificarPerfilRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.UsuarioResponse;
import ar.edu.uade.api.deliverygo.usuarios.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// RN-05: sólo el titular opera sobre /users/{id}. El control va en el método, no en una regex sobre la
// URL, para que no haya forma de llegar al handler sin pasar por él. Un id inválido se rechaza con 400
// antes de evaluar la titularidad.
@RestController
@RequestMapping("/api/v1/users")
public class UsuarioController implements IUsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @Override
    @GetMapping("/{id}")
    @PreAuthorize("principal.id() == #id")
    public ResponseEntity<UsuarioResponse> consultarPerfil(Long id) {
        return ResponseEntity.ok(usuarioService.consultarPerfil(id));
    }

    @Override
    @PutMapping("/{id}")
    @PreAuthorize("principal.id() == #id")
    public ResponseEntity<UsuarioResponse> modificarPerfil(Long id, ModificarPerfilRequest request) {
        return ResponseEntity.ok(usuarioService.modificarPerfil(id, request));
    }
}
