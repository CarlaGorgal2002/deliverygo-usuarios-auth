package org.deliverygo.controller;

import org.deliverygo.dto.CrearUsuarioDto;
import org.deliverygo.dto.UsuarioDto;
import org.deliverygo.services.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/register")
    public ResponseEntity<UsuarioDto> crearUsuario(@RequestBody CrearUsuarioDto crearUsuarioDto) {

        UsuarioDto usuarioDto = usuarioService.crearUsuario(crearUsuarioDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioDto);

    }

}
