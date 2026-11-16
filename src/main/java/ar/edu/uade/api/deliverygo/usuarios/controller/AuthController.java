package ar.edu.uade.api.deliverygo.usuarios.controller;

import ar.edu.uade.api.deliverygo.usuarios.dto.LoginRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.LoginResponse;
import ar.edu.uade.api.deliverygo.usuarios.dto.RegistrarUsuarioRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.RestablecerPasswordRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.SesionResponse;
import ar.edu.uade.api.deliverygo.usuarios.dto.SolicitarRecuperacionRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.UsuarioResponse;
import ar.edu.uade.api.deliverygo.usuarios.security.UsuarioAutenticado;
import ar.edu.uade.api.deliverygo.usuarios.service.AuthService;
import ar.edu.uade.api.deliverygo.usuarios.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController implements IAuthController {

    private final AuthService authService;
    private final UsuarioService usuarioService;

    public AuthController(AuthService authService, UsuarioService usuarioService) {
        this.authService = authService;
        this.usuarioService = usuarioService;
    }

    @Override
    @PostMapping("/register")
    public ResponseEntity<UsuarioResponse> registrar(RegistrarUsuarioRequest request) {
        UsuarioResponse usuario = usuarioService.registrarUsuario(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + usuario.id())).body(usuario);
    }

    @Override
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @Override
    @PostMapping("/password/forgot")
    public ResponseEntity<Void> solicitarRecuperacion(SolicitarRecuperacionRequest request) {
        authService.solicitarRecuperacion(request);
        return ResponseEntity.accepted().build();
    }

    @Override
    @PostMapping("/password/reset")
    public ResponseEntity<Void> restablecerPassword(RestablecerPasswordRequest request) {
        authService.restablecerPassword(request);
        return ResponseEntity.noContent().build();
    }

    // Sólo personas (SecurityConfig): una API Key de módulo no renueva ni valida sesiones de usuario.
    @Override
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> renovarSesion(UsuarioAutenticado actual) {
        return ResponseEntity.ok(authService.renovarSesion(actual));
    }

    @Override
    @GetMapping("/session")
    public ResponseEntity<SesionResponse> validarSesion(UsuarioAutenticado actual) {
        return ResponseEntity.ok(authService.validarSesion(actual));
    }
}
