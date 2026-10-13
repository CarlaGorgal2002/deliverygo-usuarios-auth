package org.deliverygo.controller;

import org.deliverygo.dto.LoginDto;
import org.deliverygo.dto.TokenDto;
import org.deliverygo.security.JwtService;
import org.deliverygo.dto.RegistrarUsuarioRequest;
import org.deliverygo.dto.UsuarioResponse;
import org.deliverygo.services.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            AuthService authService
            ) {

        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenDto> login(@RequestBody LoginDto dto) {
        return ResponseEntity.ok(authService.login(dto));
    }

    @PostMapping("/register")
    public ResponseEntity<UsuarioResponse> register(@RequestBody RegistrarUsuarioRequest usuarioRegistro) {
        UsuarioResponse usuario = authService.registrar(usuarioRegistro);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuario);
    }
}
