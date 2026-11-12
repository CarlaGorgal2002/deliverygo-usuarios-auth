package ar.edu.uade.api.deliverygo.usuarios;

import ar.edu.uade.api.deliverygo.usuarios.domain.Usuario;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// CU-06 (API-10) y CU-07 (API-11), con las reglas RN-11 y RN-12.
class AdministracionApiTest extends ApiTestSupport {

    private String estado(long id) {
        return "/api/v1/users/" + id + "/status";
    }

    private String rol(long id) {
        return "/api/v1/users/" + id + "/role";
    }

    // Rol vigente en la base, consultado por la API de sesión (CU-09); la entidad carga el rol de forma diferida.
    private String rolActual(Sesion sesion) throws Exception {
        return json.readTree(mvc.perform(get("/api/v1/auth/session").header("Authorization", sesion.bearer()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("rol").asText();
    }

    // ------------------------------------------------------------------ CU-06

    @Test
    void elAdministradorDesactivaYReactivaUnaCuenta() throws Exception {
        Sesion admin = crearConRolYLoguear("ADMIN");
        Sesion a = registrarYLoguear();

        mvc.perform(conJson(put(estado(a.id())).header("Authorization", admin.bearer()), cuerpo("estado", "INACTIVO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(a.id()))
                .andExpect(jsonPath("$.estado").value("INACTIVO"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"))
                .andExpect(jsonPath("$.password").doesNotExist());

        // RN-04: no puede iniciar sesión (con la contraseña correcta, el estado sí se informa)
        assertError(login(a.email(), PASSWORD), 403, "USER_INACTIVE", "/api/v1/auth/login");
        // y su token todavía vigente deja de servir sobre su perfil y sus direcciones
        assertError(mvc.perform(get("/api/v1/users/" + a.id()).header("Authorization", a.bearer())),
                403, "USER_INACTIVE", "/api/v1/users/" + a.id());

        mvc.perform(conJson(put(estado(a.id())).header("Authorization", admin.bearer()), cuerpo("estado", "ACTIVO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVO"));

        login(a.email(), PASSWORD).andExpect(status().isOk());
        mvc.perform(get("/api/v1/users/" + a.id()).header("Authorization", a.bearer())).andExpect(status().isOk());
    }

    @Test
    void repetirElMismoEstadoEsIdempotente() throws Exception {
        Sesion admin = crearConRolYLoguear("ADMIN");
        Sesion a = registrarYLoguear();

        for (int i = 0; i < 2; i++) {
            mvc.perform(conJson(put(estado(a.id())).header("Authorization", admin.bearer()), cuerpo("estado", "INACTIVO")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("INACTIVO"));
        }
    }

    // ------------------------------------------------------------------ RN-11

    @Test
    void sinSerAdministradorLaRespuestaEs401O403() throws Exception {
        Sesion a = registrarYLoguear();
        Sesion b = registrarYLoguear();
        Sesion comercio = crearConRolYLoguear("COMERCIO");
        String ruta = estado(b.id());

        assertError(mvc.perform(conJson(put(ruta), cuerpo("estado", "INACTIVO"))), 401, "UNAUTHENTICATED", ruta);
        assertError(mvc.perform(conJson(put(ruta).header("Authorization", a.bearer()), cuerpo("estado", "INACTIVO"))),
                403, "FORBIDDEN", ruta);
        assertError(mvc.perform(conJson(put(ruta).header("Authorization", comercio.bearer()), cuerpo("estado", "INACTIVO"))),
                403, "FORBIDDEN", ruta);
        assertError(mvc.perform(conJson(put(rol(b.id())).header("Authorization", a.bearer()), cuerpo("rol", "ADMIN"))),
                403, "FORBIDDEN", rol(b.id()));

        // un módulo con API Key tampoco administra cuentas
        assertError(mvc.perform(conJson(put(ruta).header("X-Api-Key", CLAVE_PEDIDOS), cuerpo("estado", "INACTIVO"))),
                403, "FORBIDDEN", ruta);

        // nada cambió
        mvc.perform(get("/api/v1/users/" + b.id()).header("Authorization", b.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }

    @Test
    void elRolDelTokenNoAlcanzaSeConfirmaContraLaBase() throws Exception {
        Sesion adminDegradado = crearConRolYLoguear("ADMIN");
        Sesion adminInactivo = crearConRolYLoguear("ADMIN");
        Sesion a = registrarYLoguear();

        // al primero le quitan el rol y al segundo lo desactivan después de iniciar sesión
        Usuario degradado = usuarioRepository.findByEmail(adminDegradado.email()).orElseThrow();
        degradado.setRol(rolRepository.findByNombre("CLIENTE").orElseThrow());
        usuarioRepository.save(degradado);
        marcarInactivo(adminInactivo.email());

        assertError(mvc.perform(conJson(put(estado(a.id())).header("Authorization", adminDegradado.bearer()),
                cuerpo("estado", "INACTIVO"))), 403, "FORBIDDEN", estado(a.id()));
        assertError(mvc.perform(conJson(put(estado(a.id())).header("Authorization", adminInactivo.bearer()),
                cuerpo("estado", "INACTIVO"))), 403, "USER_INACTIVE", estado(a.id()));

        assertThat(usuarioRepository.findById(a.id()).orElseThrow().estaActivo()).isTrue();
    }

    // ------------------------------------------------------------------ RN-12

    @Test
    void unAdministradorNoModificaSuPropiaCuenta() throws Exception {
        Sesion admin = crearConRolYLoguear("ADMIN");

        assertError(mvc.perform(conJson(put(estado(admin.id())).header("Authorization", admin.bearer()),
                cuerpo("estado", "INACTIVO"))), 403, "FORBIDDEN", estado(admin.id()));
        assertError(mvc.perform(conJson(put(rol(admin.id())).header("Authorization", admin.bearer()),
                cuerpo("rol", "CLIENTE"))), 403, "FORBIDDEN", rol(admin.id()));

        assertThat(usuarioRepository.findById(admin.id()).orElseThrow().estaActivo()).isTrue();
        assertThat(rolActual(admin)).isEqualTo("ADMIN");
    }

    @Test
    void dosAdministradoresQueSeDegradanAlMismoTiempoNoSeDejanSinNinguno() throws Exception {
        for (int ronda = 0; ronda < 5; ronda++) {
            Sesion uno = crearConRolYLoguear("ADMIN");
            Sesion otro = crearConRolYLoguear("ADMIN");

            List<Integer> estados = enParalelo(2, indice -> {
                Sesion actor = indice == 0 ? uno : otro;
                Sesion objetivo = indice == 0 ? otro : uno;
                return mvc.perform(conJson(put(rol(objetivo.id())).header("Authorization", actor.bearer()),
                                cuerpo("rol", "CLIENTE")))
                        .andReturn().getResponse().getStatus();
            });

            // el segundo en entrar ya no es administrador cuando le toca su turno
            assertThat(estados).containsExactlyInAnyOrder(200, 403);
            int quedan = 0;
            for (Sesion s : List.of(uno, otro)) {
                if ("ADMIN".equals(rolActual(s))) {
                    quedan++;
                }
            }
            assertThat(quedan).isEqualTo(1);
        }
    }

    // ------------------------------------------------------------------ CU-07

    @Test
    void elAdministradorAsignaRolesYElTokenViejoConservaElAnterior() throws Exception {
        Sesion admin = crearConRolYLoguear("ADMIN");
        Sesion a = registrarYLoguear();

        mvc.perform(conJson(put(rol(a.id())).header("Authorization", admin.bearer()), cuerpo("rol", "COMERCIO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(a.id()))
                .andExpect(jsonPath("$.rol").value("COMERCIO"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));

        // idempotente
        mvc.perform(conJson(put(rol(a.id())).header("Authorization", admin.bearer()), cuerpo("rol", "COMERCIO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("COMERCIO"));

        // el perfil ya muestra el rol nuevo; el login siguiente también
        mvc.perform(get("/api/v1/users/" + a.id()).header("Authorization", a.bearer()))
                .andExpect(jsonPath("$.rol").value("COMERCIO"));
        login(a.email(), PASSWORD).andExpect(jsonPath("$.rol").value("COMERCIO"));

        // un rol asignado no se reemplaza solo: ADMIN puede volver a CLIENTE
        mvc.perform(conJson(put(rol(a.id())).header("Authorization", admin.bearer()), cuerpo("rol", "CLIENTE")))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }

    // ------------------------------------------------------------------ validación

    @Test
    void losDatosInvalidosYLosRecursosInexistentesSeRechazanConElFormatoComun() throws Exception {
        Sesion admin = crearConRolYLoguear("ADMIN");
        Sesion a = registrarYLoguear();

        // usuario inexistente
        assertError(mvc.perform(conJson(put(estado(987654321L)).header("Authorization", admin.bearer()),
                cuerpo("estado", "INACTIVO"))), 404, "USER_NOT_FOUND", estado(987654321L));
        assertError(mvc.perform(conJson(put(rol(987654321L)).header("Authorization", admin.bearer()),
                cuerpo("rol", "ADMIN"))), 404, "USER_NOT_FOUND", rol(987654321L));

        // ids inválidos
        for (String id : new String[]{"abc", "0", "-3"}) {
            assertError(mvc.perform(conJson(put("/api/v1/users/" + id + "/status").header("Authorization", admin.bearer()),
                    cuerpo("estado", "INACTIVO"))), 400, "VALIDATION_ERROR", "/api/v1/users/" + id + "/status");
        }

        // cuerpos inválidos
        assertError(mvc.perform(conJson(put(estado(a.id())).header("Authorization", admin.bearer()), "{}")),
                400, "VALIDATION_ERROR", estado(a.id())).andExpect(jsonPath("$.details[0].campo").value("estado"));
        assertError(mvc.perform(conJson(put(estado(a.id())).header("Authorization", admin.bearer()), cuerpo("estado", "BORRADO"))),
                400, "VALIDATION_ERROR", estado(a.id()));
        assertError(mvc.perform(conJson(put(rol(a.id())).header("Authorization", admin.bearer()), cuerpo("rol", "SUPERUSUARIO"))),
                400, "VALIDATION_ERROR", rol(a.id())).andExpect(jsonPath("$.details[0].campo").value("rol"));
        assertError(mvc.perform(conJson(put(rol(a.id())).header("Authorization", admin.bearer()), cuerpo("rol", null))),
                400, "VALIDATION_ERROR", rol(a.id()));
        // DC-01: sólo el campo del caso de uso
        assertError(mvc.perform(conJson(put(estado(a.id())).header("Authorization", admin.bearer()),
                cuerpo("estado", "INACTIVO", "rol", "ADMIN"))), 400, "VALIDATION_ERROR", estado(a.id()))
                .andExpect(jsonPath("$.details[0].campo").value("rol"));

        mvc.perform(get("/api/v1/users/" + a.id()).header("Authorization", a.bearer()))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.rol").value("CLIENTE"));
    }
}
