package ar.edu.uade.api.deliverygo.usuarios;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// CU-10 y RN-15: los otros módulos consultan a Usuarios sin credencial (ni JWT ni clave). Sólo leen: perfil (API-03),
// direcciones (API-05) y estado y rol (API-16). El JWT queda para el front: con él rigen las reglas del titular (RN-05).
class ModulosApiTest extends ApiTestSupport {

    private static String acceso(long id) {
        return "/api/v1/users/" + id + "/access";
    }

    // ------------------------------------------------------------------ CU-10

    @Test
    void unModuloConsultaEstadoYRolSinCredencialNiDatosPersonales() throws Exception {
        Sesion a = registrarYLoguear();

        mvc.perform(get(acceso(a.id())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(a.id()))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.nombre").doesNotExist());
    }

    @Test
    void laConsultaDeUnaCuentaInactivaDevuelve200ConElEstado() throws Exception {
        Sesion a = registrarYLoguear();
        marcarInactivo(a.email());

        mvc.perform(get(acceso(a.id())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));
    }

    @Test
    void laConsultaReflejaElRolActual() throws Exception {
        Sesion admin = crearConRolYLoguear("ADMIN");
        Sesion a = registrarYLoguear();
        mvc.perform(conJson(put("/api/v1/users/" + a.id() + "/role").header("Authorization", admin.bearer()),
                cuerpo("rol", "COMERCIO"))).andExpect(status().isOk());

        mvc.perform(get(acceso(a.id()))).andExpect(jsonPath("$.rol").value("COMERCIO"));
    }

    @Test
    void lasCabecerasQueMandenLosModulosNoCambianLaRespuesta() throws Exception {
        Sesion a = registrarYLoguear();
        String ruta = "/api/v1/users/" + a.id();

        // Algunos módulos todavía mandan X-Api-Key o un Bearer vacío o viejo: se ignoran y la lectura sigue funcionando.
        mvc.perform(get(ruta).header("X-Api-Key", "cualquier-clave")).andExpect(status().isOk());
        mvc.perform(get(ruta).header("Authorization", "Bearer ")).andExpect(status().isOk());
        mvc.perform(get(ruta).header("Authorization", "Bearer token-viejo-o-de-otro-modulo")).andExpect(status().isOk());
        mvc.perform(get(acceso(a.id())).header("Authorization", "Bearer token-viejo")).andExpect(status().isOk());
    }

    @Test
    void idInexistenteEInvalido() throws Exception {
        assertError(mvc.perform(get(acceso(987654321L))), 404, "USER_NOT_FOUND", acceso(987654321L));
        for (String id : new String[]{"abc", "0", "-1"}) {
            assertError(mvc.perform(get("/api/v1/users/" + id + "/access")),
                    400, "VALIDATION_ERROR", "/api/v1/users/" + id + "/access");
        }
    }

    // ------------------------------------------------------------------ API-03 y API-05 para módulos

    @Test
    void unModuloConsultaElPerfilSinCredencialYRecibeElEstadoDeUnaCuentaInactiva() throws Exception {
        Sesion a = registrarYLoguear();
        marcarInactivo(a.email());

        mvc.perform(get("/api/v1/users/" + a.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(a.id()))
                .andExpect(jsonPath("$.email").value(a.email()))
                .andExpect(jsonPath("$.estado").value("INACTIVO"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        // el titular inactivo, en cambio, sigue recibiendo 403
        assertError(mvc.perform(get("/api/v1/users/" + a.id()).header("Authorization", a.bearer())),
                403, "USER_INACTIVE", "/api/v1/users/" + a.id());
        assertError(mvc.perform(get("/api/v1/users/987654321")), 404, "USER_NOT_FOUND", "/api/v1/users/987654321");
    }

    @Test
    void unModuloListaLasDireccionesSinCredencial() throws Exception {
        Sesion a = registrarYLoguear();
        crearDireccionComo(a, true);
        String ruta = "/api/v1/users/" + a.id() + "/addresses";

        mvc.perform(get(ruta))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].predeterminada").value(true));

        // también las recibe si la cuenta está inactiva; si el usuario no existe, 404
        marcarInactivo(a.email());
        mvc.perform(get(ruta)).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        assertError(mvc.perform(get("/api/v1/users/987654321/addresses")),
                404, "USER_NOT_FOUND", "/api/v1/users/987654321/addresses");
    }

    @Test
    void conElJwtDeOtraPersonaRigenLasReglasDelTitular() throws Exception {
        Sesion a = registrarYLoguear();
        Sesion b = registrarYLoguear();
        Sesion admin = crearConRolYLoguear("ADMIN");

        // el front manda el JWT del usuario: sólo el titular lee su perfil y sus direcciones (RN-05)
        for (String ruta : new String[]{"/api/v1/users/" + a.id(), "/api/v1/users/" + a.id() + "/addresses"}) {
            assertError(mvc.perform(get(ruta).header("Authorization", b.bearer())), 403, "FORBIDDEN", ruta);
            assertError(mvc.perform(get(ruta).header("Authorization", admin.bearer())), 403, "FORBIDDEN", ruta);
            mvc.perform(get(ruta).header("Authorization", a.bearer())).andExpect(status().isOk());
        }
    }

    @Test
    void sinCredencialNadaSeEscribe() throws Exception {
        Sesion a = registrarYLoguear();
        long id = a.id();
        String direccion = "/api/v1/users/" + id + "/addresses";

        assertError(mvc.perform(conJson(put("/api/v1/users/" + id), cuerpo("nombre", "X", "apellido", "Y", "email", emailUnico()))),
                401, "UNAUTHENTICATED", "/api/v1/users/" + id);
        assertError(mvc.perform(conJson(post(direccion),
                cuerpo("calle", "C", "numero", "1", "ciudad", "X", "provincia", "X", "codigoPostal", "1000"))),
                401, "UNAUTHENTICATED", direccion);
        assertError(mvc.perform(delete(direccion + "/1")), 401, "UNAUTHENTICATED", direccion + "/1");
        assertError(mvc.perform(patch(direccion + "/1/default")), 401, "UNAUTHENTICATED", direccion + "/1/default");
        assertError(mvc.perform(conJson(put("/api/v1/users/" + id + "/status"), cuerpo("estado", "INACTIVO"))),
                401, "UNAUTHENTICATED", "/api/v1/users/" + id + "/status");
        assertError(mvc.perform(conJson(put("/api/v1/users/" + id + "/role"), cuerpo("rol", "ADMIN"))),
                401, "UNAUTHENTICATED", "/api/v1/users/" + id + "/role");
        assertError(mvc.perform(get("/api/v1/auth/session")), 401, "UNAUTHENTICATED", "/api/v1/auth/session");
    }

    @Test
    void yaNoExisteElTokenDeModulo() throws Exception {
        String ruta = "/api/v1/auth/module-token";
        assertError(mvc.perform(conJson(post(ruta), cuerpo("modulo", "pedidos", "clave", "cualquier-clave-0123456789"))),
                401, "UNAUTHENTICATED", ruta);
    }
}
