package ar.edu.uade.api.deliverygo.usuarios.controller;

import ar.edu.uade.api.deliverygo.usuarios.dto.AccesoUsuarioResponse;
import ar.edu.uade.api.deliverygo.usuarios.dto.CambiarEstadoRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.CambiarRolRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.ModificarPerfilRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.UsuarioResponse;
import ar.edu.uade.api.deliverygo.usuarios.security.ModuloAutenticado;
import ar.edu.uade.api.deliverygo.usuarios.security.UsuarioAutenticado;
import ar.edu.uade.api.deliverygo.usuarios.service.AdministracionService;
import ar.edu.uade.api.deliverygo.usuarios.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// RN-05: sólo el titular opera sobre /users/{id}; los módulos con credencial propia sólo leen (RN-15) y la
// administración (CU-06, CU-07) exige un ADMIN (RN-11). El control va en el método, no en una regex sobre la
// URL, para que no haya forma de llegar al handler sin pasar por él. Un id inválido se rechaza con 400
// antes de evaluar la autorización.
@RestController
@RequestMapping("/api/v1/users")
public class UsuarioController implements IUsuarioController {

    private final UsuarioService usuarioService;
    private final AdministracionService administracionService;

    public UsuarioController(UsuarioService usuarioService, AdministracionService administracionService) {
        this.usuarioService = usuarioService;
        this.administracionService = administracionService;
    }

    @Override
    @GetMapping("/{id}")
    @PreAuthorize("@acceso.titularOModulo(principal, #id)")
    public ResponseEntity<UsuarioResponse> consultarPerfil(Long id, Authentication authentication) {
        boolean esModulo = authentication.getPrincipal() instanceof ModuloAutenticado;
        return ResponseEntity.ok(esModulo
                ? usuarioService.consultarPerfilParaModulo(id)
                : usuarioService.consultarPerfil(id));
    }

    @Override
    @PutMapping("/{id}")
    @PreAuthorize("@acceso.titular(principal, #id)")
    public ResponseEntity<UsuarioResponse> modificarPerfil(Long id, ModificarPerfilRequest request) {
        return ResponseEntity.ok(usuarioService.modificarPerfil(id, request));
    }

    @Override
    @PutMapping("/{id}/status")
    @PreAuthorize("@acceso.administrador(principal)")
    public ResponseEntity<UsuarioResponse> cambiarEstado(Long id, CambiarEstadoRequest request, UsuarioAutenticado administrador) {
        return ResponseEntity.ok(administracionService.cambiarEstado(administrador.id(), id, request.estado()));
    }

    @Override
    @PutMapping("/{id}/role")
    @PreAuthorize("@acceso.administrador(principal)")
    public ResponseEntity<UsuarioResponse> cambiarRol(Long id, CambiarRolRequest request, UsuarioAutenticado administrador) {
        return ResponseEntity.ok(administracionService.cambiarRol(administrador.id(), id, request.rol()));
    }

    @Override
    @GetMapping("/{id}/access")
    @PreAuthorize("@acceso.modulo(principal)")
    public ResponseEntity<AccesoUsuarioResponse> consultarAcceso(Long id) {
        return ResponseEntity.ok(usuarioService.consultarAcceso(id));
    }
}
