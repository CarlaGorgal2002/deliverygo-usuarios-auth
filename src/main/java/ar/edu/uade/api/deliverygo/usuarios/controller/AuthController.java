package ar.edu.uade.api.deliverygo.usuarios.controller;

import ar.edu.uade.api.deliverygo.usuarios.dto.LoginRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.LoginResponse;
import ar.edu.uade.api.deliverygo.usuarios.dto.RegistrarUsuarioRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.UsuarioResponse;
import ar.edu.uade.api.deliverygo.usuarios.service.AuthService;
import org.springframework.http.ResponseEntity;
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
}
