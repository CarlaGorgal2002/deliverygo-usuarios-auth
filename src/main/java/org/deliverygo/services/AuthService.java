package org.deliverygo.services;

import org.deliverygo.dto.LoginDto;
import org.deliverygo.dto.RegistrarUsuarioRequest;
import org.deliverygo.dto.TokenDto;
import org.deliverygo.dto.UsuarioResponse;
import org.deliverygo.exceptions.CredencialesInvalidasException;
import org.deliverygo.exceptions.UsuarioInactivoException;
import org.deliverygo.model.EstadoUsuario;
import org.deliverygo.model.Usuario;
import org.deliverygo.repository.UsuarioRepository;
import org.deliverygo.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String MSG_CREDENCIALES = "Credenciales inválidas";

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioService usuarioService,
                       UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UsuarioResponse registrar(RegistrarUsuarioRequest request) {
        return usuarioService.registrarUsuario(request);
    }

    @Transactional(readOnly = true)
    public TokenDto login(LoginDto dto) {

        // RN-01
        if (dto.getUsername() == null || dto.getUsername().isBlank()
                || dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new IllegalArgumentException("El email y la contraseña son obligatorios");
        }

        String email = dto.getUsername().trim().toLowerCase();

        // RN-08: mismo mensaje si el email no existe o la contraseña no coincide
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new CredencialesInvalidasException(MSG_CREDENCIALES));

        // RN-03
        if (!passwordEncoder.matches(dto.getPassword(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException(MSG_CREDENCIALES);
        }

        // RN-04: se evalúa DESPUÉS de validar la contraseña (RN-08)
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new UsuarioInactivoException("Cuenta inactiva");
        }

        // RN-09: token con vencimiento, id y rol
        String token = jwtService.generarToken(
                usuario.getId(),
                usuario.getEmail(),
                usuario.getRol().getNombre()
        );

        return new TokenDto(token, "Bearer", jwtService.getExpirationSeconds());
    }
}