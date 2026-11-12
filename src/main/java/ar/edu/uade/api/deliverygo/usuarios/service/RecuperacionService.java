package ar.edu.uade.api.deliverygo.usuarios.service;

import ar.edu.uade.api.deliverygo.usuarios.domain.TokenRecuperacion;
import ar.edu.uade.api.deliverygo.usuarios.domain.Usuario;
import ar.edu.uade.api.deliverygo.usuarios.exception.DatoInvalidoException;
import ar.edu.uade.api.deliverygo.usuarios.exception.TokenRecuperacionInvalidoException;
import ar.edu.uade.api.deliverygo.usuarios.repository.TokenRecuperacionRepository;
import ar.edu.uade.api.deliverygo.usuarios.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

// CU-08: recuperación de contraseña con token de un solo uso (RN-13). La notificación al usuario la
// dispara AuthService una vez confirmada la transacción, igual que la bienvenida.
@Service
public class RecuperacionService {

    static final String MSG_TOKEN_INVALIDO = "El token de recuperación es inválido, venció o ya fue usado";
    private static final int MAX_BYTES_PASSWORD = 72;
    private static final int BYTES_DE_TOKEN = 32;

    private final SecureRandom azar = new SecureRandom();

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacionRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final Duration vigencia;

    public RecuperacionService(UsuarioRepository usuarioRepository, TokenRecuperacionRepository tokenRepository,
                               PasswordEncoder passwordEncoder,
                               @Value("${app.recovery.token-minutes}") long minutosDeVigencia) {
        if (minutosDeVigencia <= 0) {
            throw new IllegalArgumentException("app.recovery.token-minutes debe ser mayor que cero");
        }
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.vigencia = Duration.ofMinutes(minutosDeVigencia);
    }

    // Token entregado una única vez, para pasárselo a Notificaciones.
    public record Emision(Long usuarioId, String nombre, String email, String token) {
    }

    // SRV-12 (CU-08, paso 1). Devuelve vacío si no hay cuenta o está inactiva: el llamador responde igual (RN-13).
    @Transactional
    public Optional<Emision> generarToken(String emailSolicitado) {
        Optional<Usuario> encontrado = usuarioRepository.findByEmail(Textos.normalizarEmail(emailSolicitado));
        if (encontrado.isEmpty() || !encontrado.get().estaActivo()) {
            return Optional.empty();
        }
        Usuario usuario = encontrado.get();
        LocalDateTime ahora = ahora();

        // Un pedido nuevo deja sin efecto los anteriores: sólo vale el último token enviado.
        tokenRepository.invalidarVigentes(usuario.getId(), ahora);

        byte[] aleatorio = new byte[BYTES_DE_TOKEN];
        azar.nextBytes(aleatorio);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(aleatorio);
        tokenRepository.save(new TokenRecuperacion(usuario, huella(token), ahora, ahora.plus(vigencia)));

        return Optional.of(new Emision(usuario.getId(), usuario.getNombre(), usuario.getEmail(), token));
    }

    // SRV-13 (CU-08, paso 2)
    @Transactional
    public void restablecerPassword(String token, String passwordNueva) {
        if (passwordNueva.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES_PASSWORD) {
            throw new DatoInvalidoException("password", "No puede superar " + MAX_BYTES_PASSWORD + " bytes");
        }

        String huella = huella(token.trim());
        LocalDateTime ahora = ahora();

        TokenRecuperacion guardado = tokenRepository.findByTokenHash(huella)
                .orElseThrow(() -> new TokenRecuperacionInvalidoException(MSG_TOKEN_INVALIDO));
        Usuario usuario = guardado.getUsuario();
        if (guardado.getUsadoEn() != null || !guardado.getExpiraEn().isAfter(ahora) || !usuario.estaActivo()) {
            throw new TokenRecuperacionInvalidoException(MSG_TOKEN_INVALIDO);
        }

        // Consumo atómico: si dos pedidos llegan juntos con el mismo token, sólo uno obtiene 1.
        if (tokenRepository.consumir(huella, ahora) == 0) {
            throw new TokenRecuperacionInvalidoException(MSG_TOKEN_INVALIDO);
        }

        usuario.setPasswordHash(passwordEncoder.encode(passwordNueva));
        tokenRepository.invalidarVigentes(usuario.getId(), ahora);
    }

    private static LocalDateTime ahora() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private static String huella(String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no está disponible", e);
        }
    }
}
