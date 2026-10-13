package org.deliverygo.services;

import org.deliverygo.dto.LoginDto;
import org.deliverygo.dto.RegistrarUsuarioRequest;
import org.deliverygo.dto.TokenDto;
import org.deliverygo.dto.UsuarioResponse;
import org.deliverygo.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioService usuarioService;
    private final JwtService jwtService;

    public AuthService(UsuarioService usuarioService, JwtService jwtService, AuthenticationManager authenticationManager) {
        this.usuarioService = usuarioService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
    }

    public UsuarioResponse registrar(RegistrarUsuarioRequest request) {
        return usuarioService.crearUsuario(request);
    }

    public TokenDto login (LoginDto dto) {

        Authentication authentication =
                authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                dto.username(),
                                dto.password()
                        )
                );

        String username =
                authentication.getName();

        String rol =
                authentication
                        .getAuthorities()
                        .stream()
                        .map(GrantedAuthority::getAuthority)
                        .findFirst()
                        .orElse("ROLE_USER")
                        .replace("ROLE_", "");

        String token =
                jwtService.generarToken(
                        username,
                        rol
                );

        return new TokenDto(
                        token,
                        "Bearer",
                        jwtService.getExpirationSeconds()
                );
    }
}
