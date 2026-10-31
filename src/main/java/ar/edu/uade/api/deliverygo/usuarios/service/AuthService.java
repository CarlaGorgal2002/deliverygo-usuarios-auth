package ar.edu.uade.api.deliverygo.usuarios.service;

import ar.edu.uade.api.deliverygo.usuarios.domain.Usuario;
import ar.edu.uade.api.deliverygo.usuarios.dto.LoginRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.LoginResponse;
import ar.edu.uade.api.deliverygo.usuarios.dto.RegistrarUsuarioRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.UsuarioResponse;
import ar.edu.uade.api.deliverygo.usuarios.exception.CredencialesInvalidasException;
import ar.edu.uade.api.deliverygo.usuarios.exception.UsuarioInactivoException;
import ar.edu.uade.api.deliverygo.usuarios.integration.NotificacionesClient;
import ar.edu.uade.api.deliverygo.usuarios.repository.UsuarioRepository;
import ar.edu.uade.api.deliverygo.usuarios.security.JwtService;
import ar.edu.uade.api.deliverygo.usuarios.security.TokenEmitido;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final String TIPO_TOKEN = "Bearer";
    private static final String MSG_CREDENCIALES = "Email o contraseña incorrectos";

    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final NotificacionesClient notificacionesClient;

    // Se compara contra este hash cuando el email no existe, para que el login tarde lo mismo
    // exista o no la cuenta (RN-08).
    private final String hashDeRelleno;

    public AuthService(UsuarioService usuarioService, UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService,
                       NotificacionesClient notificacionesClient) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.notificacionesClient = notificacionesClient;
        this.hashDeRelleno = passwordEncoder.encode("relleno-que-no-corresponde-a-ninguna-cuenta");
    }

    // SRV-01 (CU-01). No es transaccional: la bienvenida se pide recién cuando el alta ya está confirmada.
    public UsuarioResponse registrar(RegistrarUsuarioRequest request) {
        UsuarioResponse usuario = usuarioService.registrarUsuario(request);

        // INT-01 no bloqueante: si falla, el registro se mantiene y el cliente igual recibe el 201.
        try {
            notificacionesClient.enviarBienvenida(usuario.id(), usuario.nombre(), usuario.email());
        } catch (RuntimeException e) {
            log.warn("INT-01: no se pudo solicitar la bienvenida del usuario {}", usuario.id(), e);
        }
        return usuario;
    }

    // SRV-02 (CU-02)
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Optional<Usuario> encontrado = usuarioRepository.findByEmail(Textos.normalizarEmail(request.email()));

        // RN-03 y RN-08: email inexistente y contraseña incorrecta dan exactamente el mismo error.
        String hash = encontrado.map(Usuario::getPasswordHash).orElse(hashDeRelleno);
        boolean coincide = passwordEncoder.matches(request.password(), hash);
        if (encontrado.isEmpty() || !coincide) {
            throw new CredencialesInvalidasException(MSG_CREDENCIALES);
        }

        Usuario usuario = encontrado.get();

        // RN-04: el estado se informa sólo después de comprobar la contraseña (RN-08).
        if (!usuario.estaActivo()) {
            throw new UsuarioInactivoException(UsuarioService.MSG_INACTIVO);
        }

        // RN-09: token con id, rol y vencimiento
        TokenEmitido token = jwtService.generarToken(usuario.getId(), usuario.getRol().getNombre());
        return new LoginResponse(
                token.token(),
                TIPO_TOKEN,
                token.expiraEn().atOffset(ZoneOffset.UTC),
                usuario.getId(),
                usuario.getNombre(),
                usuario.getRol().getNombre());
    }
}
