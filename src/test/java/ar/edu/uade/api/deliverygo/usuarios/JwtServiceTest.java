package ar.edu.uade.api.deliverygo.usuarios;

import ar.edu.uade.api.deliverygo.usuarios.security.JwtService;
import ar.edu.uade.api.deliverygo.usuarios.security.TokenEmitido;
import ar.edu.uade.api.deliverygo.usuarios.security.UsuarioAutenticado;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// RN-09: el token se firma, vence y se valida sin depender del contexto de Spring.
class JwtServiceTest {

    private static final String SECRETO = "secreto-de-prueba-con-mas-de-treinta-y-dos-caracteres";

    private final JwtService servicio = new JwtService(SECRETO, 60);

    @Test
    void generaYLeeUnTokenConIdYRol() {
        TokenEmitido emitido = servicio.generarToken(42L, "COMERCIO");

        UsuarioAutenticado usuario = servicio.leer(emitido.token());

        assertThat(usuario.id()).isEqualTo(42L);
        assertThat(usuario.rol()).isEqualTo("COMERCIO");
        assertThat(emitido.expiraEn()).isAfter(Instant.now().plusSeconds(59 * 60)).isBefore(Instant.now().plusSeconds(61 * 60));
    }

    @Test
    void rechazaUnTokenFirmadoConOtraClave() {
        String ajeno = new JwtService("otra-clave-distinta-de-mas-de-treinta-y-dos-bytes", 60)
                .generarToken(1L, "ADMIN").token();

        assertThatThrownBy(() -> servicio.leer(ajeno)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaUnTokenVencido() {
        String vencido = Jwts.builder().subject("1").claim("role", "CLIENTE")
                .expiration(Date.from(Instant.now().minusSeconds(5)))
                .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8))).compact();

        assertThatThrownBy(() -> servicio.leer(vencido)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaUnTokenSinFirma() {
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        String sinFirma = b64.encodeToString("{\"alg\":\"none\"}".getBytes(StandardCharsets.UTF_8)) + "."
                + b64.encodeToString("{\"sub\":\"1\",\"role\":\"ADMIN\"}".getBytes(StandardCharsets.UTF_8)) + ".";

        assertThatThrownBy(() -> servicio.leer(sinFirma)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaUnTokenCuyoSubNoEsUnId() {
        String raro = Jwts.builder().subject("ana@example.com").claim("role", "CLIENTE")
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8))).compact();

        assertThatThrownBy(() -> servicio.leer(raro)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exigeUnaClaveDeAlMenos32BytesYUnaVigenciaPositiva() {
        assertThatThrownBy(() -> new JwtService("corta", 60)).isInstanceOf(WeakKeyException.class);
        assertThatThrownBy(() -> new JwtService(SECRETO, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
