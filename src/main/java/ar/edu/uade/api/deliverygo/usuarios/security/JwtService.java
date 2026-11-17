package ar.edu.uade.api.deliverygo.usuarios.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

// Emite y valida el JWT de la sesión de una persona (RN-09): sub = id del usuario como texto, role = su rol
// (CLIENTE, COMERCIO, REPARTIDOR, ADMIN), iat y exp. Es sólo para la integración con el front (Portal): entre
// módulos no se usa JWT, las consultas de otros módulos no llevan credencial (RN-15).
// Hoy la firma es HMAC-SHA256 con una clave compartida; la firma asimétrica es una decisión pendiente
// de acordar con los consumidores (ver doc/openapi/ACUERDOS_Y_BRECHAS.md).
@Service
public class JwtService {

    private static final String CLAIM_ROL = "role";

    private final SecretKey clave;
    private final Duration vigencia;

    // La clave sale de la configuración (JWT_SECRET); debe tener al menos 32 bytes para HS256.
    public JwtService(@Value("${app.jwt.secret}") String secreto,
                      @Value("${app.jwt.expiration-minutes}") long minutosDeVigencia) {
        if (minutosDeVigencia <= 0) {
            throw new IllegalArgumentException("La vigencia de los tokens debe ser mayor que cero");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.vigencia = Duration.ofMinutes(minutosDeVigencia);
    }

    public TokenEmitido generarToken(Long usuarioId, String rol) {
        Instant emision = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        Instant expiracion = emision.plus(vigencia);

        String token = Jwts.builder()
                .subject(String.valueOf(usuarioId))
                .claim(CLAIM_ROL, rol)
                .issuedAt(Date.from(emision))
                .expiration(Date.from(expiracion))
                .signWith(clave, Jwts.SIG.HS256)
                .compact();

        return new TokenEmitido(token, expiracion);
    }

    // Verifica firma y vencimiento. Lanza JwtException o IllegalArgumentException si el token no sirve
    // (también si el sub no es un id numérico).
    public UsuarioAutenticado leer(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(clave)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String sub = claims.getSubject();
        String rol = claims.get(CLAIM_ROL, String.class);
        if (sub == null || rol == null) {
            throw new JwtException("Token incompleto");
        }
        return new UsuarioAutenticado(Long.valueOf(sub), rol, claims.getExpiration().toInstant());
    }
}
