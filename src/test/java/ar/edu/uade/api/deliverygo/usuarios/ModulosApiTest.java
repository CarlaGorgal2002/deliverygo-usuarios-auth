package ar.edu.uade.api.deliverygo.usuarios;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// CU-10 (API-16) y el acceso de módulos consumidores a API-03 y API-05 (RN-15). Las claves de prueba
// están en ApiTestSupport: 'pedidos' y 'catalogo' son válidas; 'pagos' es demasiado corta y no entra.
class ModulosApiTest extends ApiTestSupport {

    private static String acceso(long id) {
        return "/api/v1/users/" + id + "/access";
    }

    // ------------------------------------------------------------------ CU-10

    @Test
    void unModuloConsultaEstadoYRolSinDatosPersonales() throws Exception {
        Sesion a = registrarYLoguear();

        mvc.perform(get(acceso(a.id())).header("X-Api-Key", CLAVE_CATALOGO))
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

        mvc.perform(get(acceso(a.id())).header("X-Api-Key", CLAVE_PEDIDOS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));
    }

    @Test
    void laConsultaReflejaElRolActual() throws Exception {
        Sesion admin = crearConRolYLoguear("ADMIN");
        Sesion a = registrarYLoguear();
        mvc.perform(conJson(put("/api/v1/users/" + a.id() + "/role").header("Authorization", admin.bearer()),
                cuerpo("rol", "COMERCIO"))).andExpect(status().isOk());

        mvc.perform(get(acceso(a.id())).header("X-Api-Key", CLAVE_PEDIDOS))
                .andExpect(jsonPath("$.rol").value("COMERCIO"));
    }

    @Test
    void sinCredencialDeModuloValidaLaRespuestaEs401O403() throws Exception {
        Sesion a = registrarYLoguear();
        Sesion admin = crearConRolYLoguear("ADMIN");
        String ruta = acceso(a.id());

        assertError(mvc.perform(get(ruta)), 401, "UNAUTHENTICATED", ruta);
        assertError(mvc.perform(get(ruta).header("X-Api-Key", "clave-incorrecta-que-no-existe-000")), 401, "UNAUTHENTICATED", ruta);
        // 'pagos' tiene configurada una clave de menos de 16 caracteres: queda sin acceso aunque se presente
        assertError(mvc.perform(get(ruta).header("X-Api-Key", "corta")), 401, "UNAUTHENTICATED", ruta);
        assertError(mvc.perform(get(ruta).header("X-Api-Key", "")), 401, "UNAUTHENTICATED", ruta);

        // un usuario, aunque sea el titular o ADMIN, no usa esta consulta
        assertError(mvc.perform(get(ruta).header("Authorization", a.bearer())), 403, "FORBIDDEN", ruta);
        assertError(mvc.perform(get(ruta).header("Authorization", admin.bearer())), 403, "FORBIDDEN", ruta);
    }

    @Test
    void siLlegaElJwtYLaApiKeyMandaElJwt() throws Exception {
        Sesion a = registrarYLoguear();

        // un usuario no se hace pasar por un módulo agregando una clave
        assertError(mvc.perform(get(acceso(a.id())).header("Authorization", a.bearer()).header("X-Api-Key", CLAVE_PEDIDOS)),
                403, "FORBIDDEN", acceso(a.id()));
    }

    @Test
    void idInexistenteEInvalido() throws Exception {
        assertError(mvc.perform(get(acceso(987654321L)).header("X-Api-Key", CLAVE_PEDIDOS)),
                404, "USER_NOT_FOUND", acceso(987654321L));
        for (String id : new String[]{"abc", "0", "-1"}) {
            assertError(mvc.perform(get("/api/v1/users/" + id + "/access").header("X-Api-Key", CLAVE_PEDIDOS)),
                    400, "VALIDATION_ERROR", "/api/v1/users/" + id + "/access");
        }
    }

    // ------------------------------------------------------------------ API-03 y API-05 para módulos

    @Test
    void cualquierModuloConsultaElPerfilYRecibeElEstadoDeUnaCuentaInactiva() throws Exception {
        Sesion a = registrarYLoguear();
        marcarInactivo(a.email());

        for (String clave : new String[]{CLAVE_PEDIDOS, CLAVE_CATALOGO}) {
            mvc.perform(get("/api/v1/users/" + a.id()).header("X-Api-Key", clave))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(a.id()))
                    .andExpect(jsonPath("$.estado").value("INACTIVO"))
                    .andExpect(jsonPath("$.password").doesNotExist());
        }
        // el titular inactivo, en cambio, sigue recibiendo 403
        assertError(mvc.perform(get("/api/v1/users/" + a.id()).header("Authorization", a.bearer())),
                403, "USER_INACTIVE", "/api/v1/users/" + a.id());
        assertError(mvc.perform(get("/api/v1/users/987654321").header("X-Api-Key", CLAVE_PEDIDOS)),
                404, "USER_NOT_FOUND", "/api/v1/users/987654321");
    }

    @Test
    void soloPedidosListaLasDireccionesDeUnUsuario() throws Exception {
        Sesion a = registrarYLoguear();
        crearDireccionComo(a, true);
        String ruta = "/api/v1/users/" + a.id() + "/addresses";

        mvc.perform(get(ruta).header("X-Api-Key", CLAVE_PEDIDOS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].predeterminada").value(true));

        // otro módulo con credencial válida no ve direcciones (RN-05)
        assertError(mvc.perform(get(ruta).header("X-Api-Key", CLAVE_CATALOGO)), 403, "FORBIDDEN", ruta);

        // Pedidos también las recibe si la cuenta está inactiva; si el usuario no existe, 404
        marcarInactivo(a.email());
        mvc.perform(get(ruta).header("X-Api-Key", CLAVE_PEDIDOS)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
        assertError(mvc.perform(get("/api/v1/users/987654321/addresses").header("X-Api-Key", CLAVE_PEDIDOS)),
                404, "USER_NOT_FOUND", "/api/v1/users/987654321/addresses");
    }

    @Test
    void unModuloNuncaEscribe() throws Exception {
        Sesion a = registrarYLoguear();
        long id = a.id();
        String direccion = "/api/v1/users/" + id + "/addresses";

        assertError(mvc.perform(conJson(put("/api/v1/users/" + id).header("X-Api-Key", CLAVE_PEDIDOS),
                cuerpo("nombre", "X", "apellido", "Y", "email", emailUnico()))), 403, "FORBIDDEN", "/api/v1/users/" + id);
        assertError(mvc.perform(conJson(post(direccion).header("X-Api-Key", CLAVE_PEDIDOS),
                cuerpo("calle", "C", "numero", "1", "ciudad", "X", "provincia", "X", "codigoPostal", "1000"))),
                403, "FORBIDDEN", direccion);
        assertError(mvc.perform(delete(direccion + "/1").header("X-Api-Key", CLAVE_PEDIDOS)), 403, "FORBIDDEN", direccion + "/1");
        assertError(mvc.perform(patch(direccion + "/1/default").header("X-Api-Key", CLAVE_PEDIDOS)), 403, "FORBIDDEN", direccion + "/1/default");
        assertError(mvc.perform(conJson(put("/api/v1/users/" + id + "/status").header("X-Api-Key", CLAVE_PEDIDOS),
                cuerpo("estado", "INACTIVO"))), 403, "FORBIDDEN", "/api/v1/users/" + id + "/status");
    }

    @Test
    void laApiKeyNoAbreLasRutasPublicasNiLasRompe() throws Exception {
        // una clave inválida en una ruta pública no la bloquea: el filtro no la evalúa
        registrar(emailUnico()).andExpect(status().isCreated());
        mvc.perform(conJson(post("/api/v1/auth/register").header("X-Api-Key", "clave-incorrecta-que-no-existe-000"),
                cuerpo("nombre", "Ana", "apellido", "Pérez", "email", emailUnico(), "password", PASSWORD)))
                .andExpect(status().isCreated());
    }
}
