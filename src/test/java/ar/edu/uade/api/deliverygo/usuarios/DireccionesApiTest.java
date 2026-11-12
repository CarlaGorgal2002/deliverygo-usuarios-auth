package ar.edu.uade.api.deliverygo.usuarios;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// CU-05 (API-05 a API-09): recorrido de VERIFICACION.md pasos 6 a 14, rechazos y concurrencia (RN-07).
class DireccionesApiTest extends ApiTestSupport {

    private String base(Sesion s) {
        return "/api/v1/users/" + s.id() + "/addresses";
    }

    private JsonNode crear(Sesion s, boolean predeterminada) throws Exception {
        return json.readTree(crearDireccionComo(s, predeterminada));
    }

    private JsonNode listar(Sesion s) throws Exception {
        return json.readTree(mvc.perform(get(base(s)).header("Authorization", s.bearer()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private long cantidadPredeterminadas(Sesion s) throws Exception {
        long n = 0;
        for (JsonNode d : listar(s)) {
            if (d.get("predeterminada").asBoolean()) n++;
        }
        return n;
    }

    @Test
    void recorridoCompletoDelCasoDeUso() throws Exception {
        Sesion a = registrarYLoguear();

        // 6. sin direcciones: 200 y []
        mvc.perform(get(base(a)).header("Authorization", a.bearer()))
                .andExpect(status().isOk()).andExpect(content().json("[]", true));

        // 7. D1 sin predeterminada: ni siquiera la primera lo es
        MvcResult creada = mvc.perform(conJson(post(base(a)).header("Authorization", a.bearer()),
                        cuerpo("calle", "Av. Independencia", "numero", "1200", "piso", "2", "departamento", "B",
                                "ciudad", "CABA", "provincia", "CABA", "codigoPostal", "C1099AAZ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.predeterminada").value(false))
                .andExpect(jsonPath("$.activa").doesNotExist())
                .andExpect(jsonPath("$.referencia").doesNotExist())
                .andReturn();
        JsonNode d1 = json.readTree(creada.getResponse().getContentAsString());
        assertThat(creada.getResponse().getHeader("Location")).isEqualTo(base(a) + "/" + d1.get("id").asLong());

        // 8. D2 predeterminada: D1 sigue en false
        JsonNode d2 = crear(a, true);
        assertThat(d2.get("predeterminada").asBoolean()).isTrue();
        assertThat(listar(a).get(0).get("predeterminada").asBoolean()).isFalse();

        // 9. seleccionar D1 con PATCH sin cuerpo: D1 true, D2 false
        String rutaD1 = base(a) + "/" + d1.get("id").asLong();
        mvc.perform(patch(rutaD1 + "/default").header("Authorization", a.bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(d1.get("id").asLong()))
                .andExpect(jsonPath("$.predeterminada").value(true));
        assertThat(cantidadPredeterminadas(a)).isEqualTo(1);

        // 10. repetir la selección es idempotente
        mvc.perform(patch(rutaD1 + "/default").header("Authorization", a.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.predeterminada").value(true));
        assertThat(listar(a)).hasSize(2);
        assertThat(cantidadPredeterminadas(a)).isEqualTo(1);

        // 11. PUT completo omitiendo piso y departamento: quedan null y sigue predeterminada
        mvc.perform(conJson(put(rutaD1).header("Authorization", a.bearer()),
                        cuerpo("calle", "San Martín", "numero", "S/N", "ciudad", "Rosario", "provincia", "Santa Fe",
                                "codigoPostal", "S2000")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calle").value("San Martín"))
                .andExpect(jsonPath("$.piso").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.departamento").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.predeterminada").value(true));

        // 12. DELETE: 204 sin cuerpo; la fila se conserva inactiva y desmarcada
        MvcResult baja = mvc.perform(delete(rutaD1).header("Authorization", a.bearer()))
                .andExpect(status().isNoContent()).andReturn();
        assertThat(baja.getResponse().getContentLength()).isZero();
        assertThat(baja.getResponse().getContentAsString()).isEmpty();
        var enBase = direccionRepository.findById(d1.get("id").asLong()).orElseThrow();
        assertThat(enBase.isActiva()).isFalse();
        assertThat(enBase.isPredeterminada()).isFalse();

        // 13. sólo queda D2, sin predeterminada: no se elige otra automáticamente
        JsonNode restantes = listar(a);
        assertThat(restantes).hasSize(1);
        assertThat(restantes.get(0).get("id").asLong()).isEqualTo(d2.get("id").asLong());
        assertThat(cantidadPredeterminadas(a)).isZero();

        // 14. repetir el DELETE: 404
        assertError(mvc.perform(delete(rutaD1).header("Authorization", a.bearer())),
                404, "ADDRESS_NOT_FOUND", rutaD1);
        assertThat(direccionRepository.findById(d1.get("id").asLong()).orElseThrow().isActiva()).isFalse();
    }

    @Test
    void unaDireccionAjenaSeTrataComoInexistente() throws Exception {
        Sesion a = registrarYLoguear();
        Sesion b = registrarYLoguear();
        long idDeB = crear(b, false).get("id").asLong();
        String ruta = base(a) + "/" + idDeB;   // dirección de B bajo la ruta de A, con el token de A

        assertError(mvc.perform(delete(ruta).header("Authorization", a.bearer())), 404, "ADDRESS_NOT_FOUND", ruta);
        assertError(mvc.perform(patch(ruta + "/default").header("Authorization", a.bearer())),
                404, "ADDRESS_NOT_FOUND", ruta + "/default");
        assertError(mvc.perform(conJson(put(ruta).header("Authorization", a.bearer()),
                        cuerpo("calle", "X", "numero", "1", "ciudad", "X", "provincia", "X", "codigoPostal", "1"))),
                404, "ADDRESS_NOT_FOUND", ruta);
        // y la dirección de B quedó intacta
        assertThat(direccionRepository.findById(idDeB).orElseThrow().isActiva()).isTrue();
    }

    @Test
    void elPutDeDireccionNoAceptaPredeterminadaNiActiva() throws Exception {
        Sesion a = registrarYLoguear();
        long id = crear(a, false).get("id").asLong();
        String ruta = base(a) + "/" + id;

        for (String campo : new String[]{"predeterminada", "activa", "referencia", "usuarioId", "id"}) {
            assertError(mvc.perform(conJson(put(ruta).header("Authorization", a.bearer()),
                            cuerpo("calle", "X", "numero", "1", "ciudad", "X", "provincia", "X", "codigoPostal", "1",
                                    campo, true))),
                    400, "VALIDATION_ERROR", ruta)
                    .andExpect(jsonPath("$.details[0].campo").value(campo));
        }
    }

    @Test
    void laAltaValidaLosCamposObligatoriosYLosLimites() throws Exception {
        Sesion a = registrarYLoguear();

        assertError(mvc.perform(conJson(post(base(a)).header("Authorization", a.bearer()),
                        cuerpo("calle", "", "numero", "n".repeat(11), "ciudad", "CABA"))),
                400, "VALIDATION_ERROR", base(a))
                .andExpect(jsonPath("$.details[?(@.campo=='calle')]").exists())
                .andExpect(jsonPath("$.details[?(@.campo=='numero')]").exists())
                .andExpect(jsonPath("$.details[?(@.campo=='provincia')]").exists())
                .andExpect(jsonPath("$.details[?(@.campo=='codigoPostal')]").exists());
        assertThat(listar(a)).isEmpty();
    }

    @Test
    void laAltaRechazaUnTipoIncorrectoSinConvertirloEnSilencio() throws Exception {
        Sesion a = registrarYLoguear();

        assertError(mvc.perform(conJson(post(base(a)).header("Authorization", a.bearer()),
                        cuerpo("calle", "X", "numero", "1", "ciudad", "X", "provincia", "X", "codigoPostal", "1",
                                "predeterminada", "true"))),
                400, "VALIDATION_ERROR", base(a))
                .andExpect(jsonPath("$.details[0].campo").value("predeterminada"));
        assertThat(listar(a)).isEmpty();
    }

    @Test
    void sinTokenLasOperacionesDeDireccionesDan401() throws Exception {
        assertError(mvc.perform(get("/api/v1/users/1/addresses")), 401, "UNAUTHENTICATED", "/api/v1/users/1/addresses");
        assertError(mvc.perform(patch("/api/v1/users/1/addresses/1/default")), 401, "UNAUTHENTICATED",
                "/api/v1/users/1/addresses/1/default");
    }

    @Test
    void ocultaLasDireccionesDeUnUsuarioInactivoEnLasEscrituras() throws Exception {
        Sesion a = registrarYLoguear();
        long id = crear(a, false).get("id").asLong();
        marcarInactivo(a.email());

        assertError(mvc.perform(delete(base(a) + "/" + id).header("Authorization", a.bearer())),
                403, "USER_INACTIVE", base(a) + "/" + id);
        assertError(mvc.perform(conJson(post(base(a)).header("Authorization", a.bearer()),
                        cuerpo("calle", "X", "numero", "1", "ciudad", "X", "provincia", "X", "codigoPostal", "1"))),
                403, "USER_INACTIVE", base(a));
    }

    // ------------------------------------------------------------------ concurrencia (RN-07)

    @Test
    void seleccionesSimultaneasDejanComoMaximoUnaPredeterminada() throws Exception {
        Sesion a = registrarYLoguear();
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            ids.add(crear(a, false).get("id").asLong());
        }

        ejecutarEnParalelo(ids.size() * 2, indice -> mvc.perform(
                        patch(base(a) + "/" + ids.get(indice % ids.size()) + "/default").header("Authorization", a.bearer()))
                .andReturn().getResponse().getStatus());

        assertThat(cantidadPredeterminadas(a)).isEqualTo(1);
    }

    @Test
    void altasSimultaneasConPredeterminadaDejanSoloUna() throws Exception {
        Sesion a = registrarYLoguear();

        List<Integer> estados = ejecutarEnParalelo(8, indice -> mvc.perform(conJson(
                        post(base(a)).header("Authorization", a.bearer()),
                        cuerpo("calle", "Calle " + indice, "numero", "1", "ciudad", "X", "provincia", "X",
                                "codigoPostal", "1000", "predeterminada", true)))
                .andReturn().getResponse().getStatus());

        assertThat(estados).containsOnly(201);
        assertThat(listar(a)).hasSize(8);
        assertThat(cantidadPredeterminadas(a)).isEqualTo(1);
    }

    @FunctionalInterface
    private interface Tarea {
        int ejecutar(int indice) throws Exception;
    }

    private List<Integer> ejecutarEnParalelo(int cantidad, Tarea tarea) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(cantidad);
        CountDownLatch largada = new CountDownLatch(1);
        List<Future<Integer>> futuros = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            int indice = i;
            futuros.add(pool.submit(() -> {
                largada.await();
                return tarea.ejecutar(indice);
            }));
        }
        largada.countDown();
        List<Integer> resultados = new ArrayList<>();
        for (Future<Integer> futuro : futuros) {
            resultados.add(futuro.get());
        }
        pool.shutdown();
        return resultados;
    }
}
