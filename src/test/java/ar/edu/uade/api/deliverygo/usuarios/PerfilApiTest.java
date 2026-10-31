package ar.edu.uade.api.deliverygo.usuarios;

import ar.edu.uade.api.deliverygo.usuarios.domain.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// CU-03 (API-03), CU-04 (API-04) y las reglas de seguridad RN-04, RN-05 y RN-09.
class PerfilApiTest extends ApiTestSupport {

    @Value("${app.jwt.secret}")
    private String secreto;

    // ------------------------------------------------------------------ CU-03

    @Test
    void elTitularConsultaSuPerfil() throws Exception {
        Sesion a = registrarYLoguear();

        mvc.perform(get("/api/v1/users/" + a.id()).header("Authorization", a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(a.id()))
                .andExpect(jsonPath("$.email").value(a.email()))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void sinTokenOConUnTokenInvalidoLaRespuestaEs401ConElFormatoComun() throws Exception {
        Sesion a = registrarYLoguear();
        String ruta = "/api/v1/users/" + a.id();

        assertError(mvc.perform(get(ruta)), 401, "UNAUTHENTICATED", ruta)
                .andExpect(header().string("WWW-Authenticate", org.hamcrest.Matchers.startsWith("Bearer")));

        // firma alterada
        String alterado = a.token().substring(0, a.token().length() - 3) + "abc";
        assertError(mvc.perform(get(ruta).header("Authorization", "Bearer " + alterado)), 401, "UNAUTHENTICATED", ruta);

        // basura
        assertError(mvc.perform(get(ruta).header("Authorization", "Bearer esto-no-es-un-jwt")), 401, "UNAUTHENTICATED", ruta);

        // vencido
        SecretKey clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        String vencido = Jwts.builder().subject(String.valueOf(a.id())).claim("role", "CLIENTE")
                .issuedAt(Date.from(Instant.now().minusSeconds(7200)))
                .expiration(Date.from(Instant.now().minusSeconds(3600)))
                .signWith(clave).compact();
        assertError(mvc.perform(get(ruta).header("Authorization", "Bearer " + vencido)), 401, "UNAUTHENTICATED", ruta);

        // firmado con otra clave
        String ajeno = Jwts.builder().subject(String.valueOf(a.id())).claim("role", "ADMIN")
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(Keys.hmacShaKeyFor("otra-clave-que-no-es-la-del-modulo-1234567890".getBytes(StandardCharsets.UTF_8)))
                .compact();
        assertError(mvc.perform(get(ruta).header("Authorization", "Bearer " + ajeno)), 401, "UNAUTHENTICATED", ruta);
    }

    @Test
    void unTokenNoSirveParaElPerfilDeOtroUsuarioNiSiquieraSiEsAdmin() throws Exception {
        Sesion a = registrarYLoguear();
        Sesion b = registrarYLoguear();
        Sesion admin = crearConRolYLoguear("ADMIN");
        Sesion comercio = crearConRolYLoguear("COMERCIO");
        String rutaB = "/api/v1/users/" + b.id();

        assertError(mvc.perform(get(rutaB).header("Authorization", a.bearer())), 403, "FORBIDDEN", rutaB);
        // RN-05: ADMIN o COMERCIO humanos no equivalen a un módulo consumidor
        assertError(mvc.perform(get(rutaB).header("Authorization", admin.bearer())), 403, "FORBIDDEN", rutaB);
        assertError(mvc.perform(get(rutaB).header("Authorization", comercio.bearer())), 403, "FORBIDDEN", rutaB);
        assertError(mvc.perform(put(rutaB).header("Authorization", a.bearer())
                        .contentType("application/json").content(cuerpo("nombre", "X", "apellido", "Y", "email", emailUnico()))),
                403, "FORBIDDEN", rutaB);
        assertError(mvc.perform(get(rutaB + "/addresses").header("Authorization", a.bearer())),
                403, "FORBIDDEN", rutaB + "/addresses");
    }

    @Test
    void unIdInvalidoDa400DespuesDeAutenticar() throws Exception {
        Sesion a = registrarYLoguear();

        for (String id : new String[]{"abc", "0", "-5", "1.5", "9223372036854775808"}) {
            assertError(mvc.perform(get("/api/v1/users/" + id).header("Authorization", a.bearer())),
                    400, "VALIDATION_ERROR", "/api/v1/users/" + id)
                    .andExpect(jsonPath("$.details[0].campo").value("id"));
        }
        // sin autenticar, el control de identidad va primero
        assertError(mvc.perform(get("/api/v1/users/abc")), 401, "UNAUTHENTICATED", "/api/v1/users/abc");
    }

    @Test
    void elTitularInactivoRecibe403UserInactive() throws Exception {
        Sesion a = registrarYLoguear();
        marcarInactivo(a.email());

        // el token sigue siendo válido (CU-06 todavía no existe), pero la cuenta no puede operar
        assertError(mvc.perform(get("/api/v1/users/" + a.id()).header("Authorization", a.bearer())),
                403, "USER_INACTIVE", "/api/v1/users/" + a.id());
        assertError(mvc.perform(get("/api/v1/users/" + a.id() + "/addresses").header("Authorization", a.bearer())),
                403, "USER_INACTIVE", "/api/v1/users/" + a.id() + "/addresses");
    }

    @Test
    void unTokenValidoDeUnUsuarioInexistenteDa404() throws Exception {
        Sesion a = registrarYLoguear();
        usuarioRepository.deleteById(a.id());

        assertError(mvc.perform(get("/api/v1/users/" + a.id()).header("Authorization", a.bearer())),
                404, "USER_NOT_FOUND", "/api/v1/users/" + a.id());
    }

    // ------------------------------------------------------------------ CU-04

    @Test
    void elPutReemplazaLosDatosEditablesYBorraElTelefonoOmitido() throws Exception {
        Sesion a = registrarYLoguear();
        String nuevoEmail = emailUnico();

        mvc.perform(put("/api/v1/users/" + a.id()).header("Authorization", a.bearer())
                        .contentType("application/json")
                        .content(cuerpo("nombre", " Anita ", "apellido", "Gómez", "email", nuevoEmail.toUpperCase().replace("TEST-", "test-"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Anita"))
                .andExpect(jsonPath("$.telefono").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));

        Usuario guardado = usuarioRepository.findById(a.id()).orElseThrow();
        assertThat(guardado.getEmail()).isEqualTo(nuevoEmail.toLowerCase());
        assertThat(guardado.getTelefono()).isNull();

        // el token sigue valiendo: su sub es el id, no el email
        mvc.perform(get("/api/v1/users/" + a.id()).header("Authorization", a.bearer())).andExpect(status().isOk());
        // y el login ya sólo funciona con el email nuevo
        login(nuevoEmail, PASSWORD).andExpect(status().isOk());
        login(a.email(), PASSWORD).andExpect(status().isUnauthorized());
    }

    @Test
    void repetirElMismoPutConservaElResultado() throws Exception {
        Sesion a = registrarYLoguear();
        String body = cuerpo("nombre", "Ana", "apellido", "Pérez", "email", a.email(), "telefono", "+54 11 4444 0000");

        for (int i = 0; i < 2; i++) {
            mvc.perform(put("/api/v1/users/" + a.id()).header("Authorization", a.bearer())
                            .contentType("application/json").content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value(a.email()))
                    .andExpect(jsonPath("$.telefono").value("+54 11 4444 0000"));
        }
    }

    @Test
    void elPutNoEsParcialYNoAceptaCamposProtegidos() throws Exception {
        Sesion a = registrarYLoguear();
        String ruta = "/api/v1/users/" + a.id();

        // sólo nombre: faltan apellido y email
        assertError(mvc.perform(put(ruta).header("Authorization", a.bearer())
                        .contentType("application/json").content(cuerpo("nombre", "Solo"))),
                400, "VALIDATION_ERROR", ruta)
                .andExpect(jsonPath("$.details[?(@.campo=='apellido')]").exists())
                .andExpect(jsonPath("$.details[?(@.campo=='email')]").exists());

        // RN-10: ni contraseña, ni rol, ni estado
        for (String campo : new String[]{"password", "rol", "estado"}) {
            assertError(mvc.perform(put(ruta).header("Authorization", a.bearer())
                            .contentType("application/json")
                            .content(cuerpo("nombre", "Ana", "apellido", "Pérez", "email", a.email(), campo, "ADMIN"))),
                    400, "VALIDATION_ERROR", ruta)
                    .andExpect(jsonPath("$.details[0].campo").value(campo));
        }
        mvc.perform(get(ruta).header("Authorization", a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));
    }

    @Test
    void elPutConElEmailDeOtroUsuarioDa409YNoCambiaNada() throws Exception {
        Sesion a = registrarYLoguear();
        Sesion b = registrarYLoguear();
        String ruta = "/api/v1/users/" + a.id();

        assertError(mvc.perform(put(ruta).header("Authorization", a.bearer()).contentType("application/json")
                        .content(cuerpo("nombre", "Cambiado", "apellido", "Cambiado", "email", b.email().toUpperCase()))),
                409, "EMAIL_ALREADY_REGISTERED", ruta);

        Usuario guardado = usuarioRepository.findById(a.id()).orElseThrow();
        assertThat(guardado.getNombre()).isEqualTo("Ana");
        assertThat(guardado.getEmail()).isEqualTo(a.email());
    }

    // ------------------------------------------------------------------ rutas fuera del contrato

    @Test
    void lasRutasInexistentesYLosMetodosNoPermitidosUsanElFormatoComun() throws Exception {
        Sesion a = registrarYLoguear();

        assertError(mvc.perform(get("/api/v1/nada").header("Authorization", a.bearer())),
                404, "NOT_FOUND", "/api/v1/nada");
        assertError(mvc.perform(delete("/api/v1/users/" + a.id()).header("Authorization", a.bearer())),
                405, "METHOD_NOT_ALLOWED", "/api/v1/users/" + a.id());
        assertError(mvc.perform(post("/api/v1/auth/login").contentType("text/plain").content("x")),
                415, "UNSUPPORTED_MEDIA_TYPE", "/api/v1/auth/login");
    }
}
