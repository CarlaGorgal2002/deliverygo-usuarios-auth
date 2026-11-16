package ar.edu.uade.api.deliverygo.usuarios.controller;

import ar.edu.uade.api.deliverygo.usuarios.dto.AccesoUsuarioResponse;
import ar.edu.uade.api.deliverygo.usuarios.dto.CambiarEstadoRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.CambiarRolRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.ModificarPerfilRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.UsuarioResponse;
import ar.edu.uade.api.deliverygo.usuarios.exception.AccesoProhibidoException;
import ar.edu.uade.api.deliverygo.usuarios.security.ModuloAutenticado;
import ar.edu.uade.api.deliverygo.usuarios.security.UsuarioAutenticado;
import ar.edu.uade.api.deliverygo.usuarios.service.AdministracionService;
import ar.edu.uade.api.deliverygo.usuarios.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Los permisos por rol están en SecurityConfig: administración (CU-06, CU-07) sólo ADMIN (RN-11) y
// CU-10 sólo módulos (RN-15). Acá se controla lo que depende del {id}: sólo el titular opera sobre su
// cuenta (RN-05); un módulo con credencial puede leer el perfil.
@RestController
@RequestMapping("/api/v1/users")
public class UsuarioController implements IUsuarioController {

    static final String MSG_PROHIBIDO = "No tiene permiso para acceder a este recurso";

    private final UsuarioService usuarioService;
    private final AdministracionService administracionService;

    public UsuarioController(UsuarioService usuarioService, AdministracionService administracionService) {
        this.usuarioService = usuarioService;
        this.administracionService = administracionService;
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> consultarPerfil(Long id, Authentication authentication) {
        if (authentication.getPrincipal() instanceof ModuloAutenticado) {
            return ResponseEntity.ok(usuarioService.consultarPerfilParaModulo(id));
        }
        verificarTitular(authentication, id);
        return ResponseEntity.ok(usuarioService.consultarPerfil(id));
    }

    @Override
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> modificarPerfil(Long id, ModificarPerfilRequest request,
                                                           Authentication authentication) {
        verificarTitular(authentication, id);
        return ResponseEntity.ok(usuarioService.modificarPerfil(id, request));
    }

    @Override
    @PutMapping("/{id}/status")
    public ResponseEntity<UsuarioResponse> cambiarEstado(Long id, CambiarEstadoRequest request, UsuarioAutenticado administrador) {
        return ResponseEntity.ok(administracionService.cambiarEstado(administrador.id(), id, request.estado()));
    }

    @Override
    @PutMapping("/{id}/role")
    public ResponseEntity<UsuarioResponse> cambiarRol(Long id, CambiarRolRequest request, UsuarioAutenticado administrador) {
        return ResponseEntity.ok(administracionService.cambiarRol(administrador.id(), id, request.rol()));
    }

    @Override
    @GetMapping("/{id}/access")
    public ResponseEntity<AccesoUsuarioResponse> consultarAcceso(Long id) {
        return ResponseEntity.ok(usuarioService.consultarAcceso(id));
    }

    // RN-05: el token tiene que ser de una persona y su id tiene que coincidir con el de la ruta.
    static void verificarTitular(Authentication authentication, Long id) {
        if (!(authentication.getPrincipal() instanceof UsuarioAutenticado usuario) || !usuario.id().equals(id)) {
            throw new AccesoProhibidoException(MSG_PROHIBIDO);
        }
    }
}
