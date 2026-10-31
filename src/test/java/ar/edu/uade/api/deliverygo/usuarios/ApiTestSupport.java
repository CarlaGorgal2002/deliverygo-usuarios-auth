package ar.edu.uade.api.deliverygo.usuarios;

import ar.edu.uade.api.deliverygo.usuarios.domain.EstadoUsuario;
import ar.edu.uade.api.deliverygo.usuarios.domain.Usuario;
import ar.edu.uade.api.deliverygo.usuarios.repository.DireccionRepository;
import ar.edu.uade.api.deliverygo.usuarios.repository.RolRepository;
import ar.edu.uade.api.deliverygo.usuarios.repository.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Utilidades comunes a las pruebas HTTP: levantan la aplicación completa con H2 en memoria.
@SpringBootTest
@AutoConfigureMockMvc
abstract class ApiTestSupport {

    static final String PASSWORD = "ClaveDemo2026!";

    @Autowired
    protected MockMvc mvc;
    @Autowired
    protected ObjectMapper json;
    @Autowired
    protected UsuarioRepository usuarioRepository;
    @Autowired
    protected RolRepository rolRepository;
    @Autowired
    protected DireccionRepository direccionRepository;
    @Autowired
    protected PasswordEncoder passwordEncoder;

    // Usuario de prueba ya registrado y con sesión iniciada.
    record Sesion(long id, String email, String token) {
        String bearer() {
            return "Bearer " + token;
        }
    }

    static String emailUnico() {
        return "test-" + UUID.randomUUID() + "@example.com";
    }

    // Arma un cuerpo JSON a partir de pares clave/valor; admite valores null.
    protected String cuerpo(Object... clavesYValores) throws Exception {
        Map<String, Object> mapa = new LinkedHashMap<>();
        for (int i = 0; i < clavesYValores.length; i += 2) {
            mapa.put((String) clavesYValores[i], clavesYValores[i + 1]);
        }
        return json.writeValueAsString(mapa);
    }

    protected MockHttpServletRequestBuilder conJson(MockHttpServletRequestBuilder request, String cuerpo) {
        return request.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    protected ResultActions registrar(String email) throws Exception {
        return mvc.perform(conJson(post("/api/v1/auth/register"),
                cuerpo("nombre", "Ana", "apellido", "Pérez", "email", email, "telefono", "+54 11 5555 0101",
                        "password", PASSWORD)));
    }

    protected ResultActions login(String email, String password) throws Exception {
        return mvc.perform(conJson(post("/api/v1/auth/login"), cuerpo("email", email, "password", password)));
    }

    protected Sesion registrarYLoguear() throws Exception {
        String email = emailUnico();
        registrar(email).andExpect(status().isCreated());
        JsonNode sesion = json.readTree(login(email, PASSWORD).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        return new Sesion(sesion.get("usuarioId").asLong(), email, sesion.get("token").asText());
    }

    // Crea directamente en la base un usuario con otro rol (el registro público sólo crea CLIENTE).
    protected Sesion crearConRolYLoguear(String rol) throws Exception {
        String email = emailUnico();
        usuarioRepository.save(new Usuario("Rol", rol, email, passwordEncoder.encode(PASSWORD), null,
                rolRepository.findByNombre(rol).orElseThrow()));
        JsonNode sesion = json.readTree(login(email, PASSWORD).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        return new Sesion(sesion.get("usuarioId").asLong(), email, sesion.get("token").asText());
    }

    protected void marcarInactivo(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email).orElseThrow();
        usuario.setEstado(EstadoUsuario.INACTIVO);
        usuarioRepository.save(usuario);
    }

    // Comprueba el formato común DTO-08.
    protected ResultActions assertError(ResultActions resultado, int status, String code, String path) throws Exception {
        return resultado
                .andExpect(status().is(status))
                .andExpect(jsonPath("$.status").value(status))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.path").value(path))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    protected String crearDireccionComo(Sesion sesion, boolean predeterminada) throws Exception {
        return mvc.perform(conJson(post("/api/v1/users/" + sesion.id() + "/addresses")
                        .header("Authorization", sesion.bearer()),
                        cuerpo("calle", "Av. Independencia", "numero", "1200", "ciudad", "CABA", "provincia", "CABA",
                                "codigoPostal", "C1099AAZ", "predeterminada", predeterminada)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }
}
