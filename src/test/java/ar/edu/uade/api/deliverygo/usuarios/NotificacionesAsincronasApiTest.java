package ar.edu.uade.api.deliverygo.usuarios;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// RN-13 con NOTIFICACIONES_MODO=http: Notificaciones tarda en responder y aun así la recuperación y el registro
// contestan sin esperarla. Si el envío fuera sincrónico, el tiempo de respuesta delataría qué emails existen.
class NotificacionesAsincronasApiTest extends ApiTestSupport {

    private static final long DEMORA_MS = 2500;
    private static final long LIMITE_RESPUESTA_MS = 1500;

    private static final String PLANTILLAS = """
            [{"id": 7, "nombre": "USUARIO_BIENVENIDA_EMAIL", "activa": true, "canal": "EMAIL"},
             {"id": 9, "nombre": "RECUPERACION_PASSWORD_EMAIL", "activa": true, "canal": "EMAIL"}]
            """;

    // Cuerpo y X-Correlation-Id de cada POST /api/v1/notifications que recibe la Notificaciones falsa.
    record Envio(String cuerpo, String correlationId) {
    }

    private static final BlockingQueue<Envio> ENVIOS = new LinkedBlockingQueue<>();
    private static final HttpServer NOTIFICACIONES = levantarNotificacionesLenta();

    @DynamicPropertySource
    static void apuntarANotificaciones(DynamicPropertyRegistry registro) {
        registro.add("app.notificaciones.modo", () -> "http");
        registro.add("app.notificaciones.base-url",
                () -> "http://127.0.0.1:" + NOTIFICACIONES.getAddress().getPort());
        registro.add("app.notificaciones.timeout-ms", () -> "5000");
    }

    @AfterAll
    static void apagar() {
        NOTIFICACIONES.stop(0);
    }

    @Test
    void laRecuperacionDeUnEmailExistenteNoEsperaANotificaciones() throws Exception {
        String email = emailUnico();
        registrar(email).andExpect(status().isCreated());
        esperarEnvio("\"plantillaId\":7");

        long inicio = System.nanoTime();
        mvc.perform(conJson(post("/api/v1/auth/password/forgot"), cuerpo("email", email))
                        .header("X-Correlation-Id", "corr-rn13"))
                .andExpect(status().isAccepted());
        long demoraMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);

        assertThat(demoraMs).isLessThan(LIMITE_RESPUESTA_MS);
        // el envío igual se hace, en segundo plano y con la correlación de la solicitud original
        Envio recuperacion = esperarEnvio("\"plantillaId\":9");
        assertThat(recuperacion.correlationId()).isEqualTo("corr-rn13");
    }

    @Test
    void elRegistroNoEsperaANotificaciones() throws Exception {
        long inicio = System.nanoTime();
        registrar(emailUnico()).andExpect(status().isCreated());
        long demoraMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);

        assertThat(demoraMs).isLessThan(LIMITE_RESPUESTA_MS);
        esperarEnvio("\"plantillaId\":7");
    }

    private static Envio esperarEnvio(String fragmento) throws InterruptedException {
        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(15);
        while (System.nanoTime() < limite) {
            Envio envio = ENVIOS.poll(500, TimeUnit.MILLISECONDS);
            if (envio != null && envio.cuerpo().replace(" ", "").contains(fragmento)) {
                return envio;
            }
        }
        throw new AssertionError("Notificaciones no recibió un envío con " + fragmento);
    }

    // POST /notifications demora DEMORA_MS antes de responder 201, aunque la plantilla ya esté en caché.
    private static HttpServer levantarNotificacionesLenta() {
        try {
            HttpServer servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            servidor.setExecutor(Executors.newCachedThreadPool());
            servidor.createContext("/api/v1/templates", intercambio -> responder(intercambio, 200, PLANTILLAS));
            servidor.createContext("/api/v1/notifications", intercambio -> {
                String cuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                dormir(DEMORA_MS);
                ENVIOS.add(new Envio(cuerpo, intercambio.getRequestHeaders().getFirst("X-Correlation-Id")));
                responder(intercambio, 201, """
                        {"id": 1, "estado": "PENDIENTE"}
                        """);
            });
            servidor.start();
            return servidor;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void responder(HttpExchange intercambio, int estado, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().set("Content-Type", "application/json");
        intercambio.sendResponseHeaders(estado, bytes.length);
        intercambio.getResponseBody().write(bytes);
        intercambio.close();
    }
}
