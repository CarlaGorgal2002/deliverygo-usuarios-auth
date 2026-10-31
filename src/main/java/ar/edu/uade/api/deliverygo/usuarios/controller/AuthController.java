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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController implements IAuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    @PostMapping("/register")
    public ResponseEntity<UsuarioResponse> registrar(RegistrarUsuarioRequest request) {
        UsuarioResponse usuario = authService.registrar(request);
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

    // Sólo personas: una API Key de módulo no renueva ni valida sesiones de usuario.
    @Override
    @PostMapping("/refresh")
    @PreAuthorize("@acceso.usuario(principal)")
    public ResponseEntity<LoginResponse> renovarSesion(UsuarioAutenticado actual) {
        return ResponseEntity.ok(authService.renovarSesion(actual));
    }

    @Override
    @GetMapping("/session")
    @PreAuthorize("@acceso.usuario(principal)")
    public ResponseEntity<SesionResponse> validarSesion(UsuarioAutenticado actual) {
        return ResponseEntity.ok(authService.validarSesion(actual));
    }
}
