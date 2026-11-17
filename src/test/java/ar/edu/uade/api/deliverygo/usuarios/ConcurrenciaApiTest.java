package ar.edu.uade.api.deliverygo.usuarios;

import ar.edu.uade.api.deliverygo.usuarios.dto.CrearDireccionRequest;
import ar.edu.uade.api.deliverygo.usuarios.exception.TokenRecuperacionInvalidoException;
import ar.edu.uade.api.deliverygo.usuarios.service.DireccionService;
import ar.edu.uade.api.deliverygo.usuarios.service.RecuperacionService;
import ar.edu.uade.api.deliverygo.usuarios.repository.TokenRecuperacionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Cada tarea usa su propia transacción real. Comprueba las reglas ante solicitudes simultáneas,
// algo que un recorrido secuencial de Postman no detecta.
class ConcurrenciaApiTest extends ApiTestSupport {
    @Autowired
    private RecuperacionService recuperacionService;
    @Autowired
    private DireccionService direccionService;
    @Autowired
    private TokenRecuperacionRepository tokenRepository;

    @Test
    void unTokenDeRecuperacionSoloSeConsumeUnaVezAunqueLleguenVariosPedidos() throws Exception {
        Sesion usuario = registrarYLoguear();
        String token = recuperacionService.generarToken(usuario.email()).orElseThrow().token();

        List<Boolean> resultados = simultaneamente(() -> {
            try {
                recuperacionService.restablecerPassword(token, "NuevaClave2026!");
                return true;
            } catch (TokenRecuperacionInvalidoException e) {
                return false;
            }
        });

        assertEquals(1, resultados.stream().filter(Boolean::booleanValue).count());
        assertTrue(passwordEncoder.matches("NuevaClave2026!",
                usuarioRepository.findById(usuario.id()).orElseThrow().getPasswordHash()));
    }

    @Test
    void altasSimultaneasNoDejanMasDeUnaDireccionPredeterminada() throws Exception {
        Sesion usuario = registrarYLoguear();
        CrearDireccionRequest direccion = new CrearDireccionRequest(
                "Lima", "100", null, null, "CABA", "CABA", "1073", true);

        simultaneamente(() -> direccionService.crearDireccion(usuario.id(), direccion).id());

        assertEquals(6, direccionService.listarDirecciones(usuario.id()).size());
        assertEquals(1, direccionRepository.findByUsuarioIdAndPredeterminadaTrue(usuario.id()).size());
    }

    @Test
    void solicitudesSimultaneasDeRecuperacionDejanSoloElUltimoTokenVigente() throws Exception {
        Sesion usuario = registrarYLoguear();

        simultaneamente(() -> recuperacionService.generarToken(usuario.email()).orElseThrow());

        long vigentes = tokenRepository.findAll().stream()
                .filter(t -> t.getUsuario().getId().equals(usuario.id()) && t.getUsadoEn() == null)
                .count();
        assertEquals(1, vigentes);
    }

    private <T> List<T> simultaneamente(Callable<T> tarea) throws Exception {
        CountDownLatch listos = new CountDownLatch(6);
        CountDownLatch inicio = new CountDownLatch(1);
        try (var ejecutor = Executors.newFixedThreadPool(6)) {
            List<Future<T>> pendientes = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                pendientes.add(ejecutor.submit(() -> {
                    listos.countDown();
                    if (!inicio.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("No se habilitó el inicio de la prueba");
                    }
                    return tarea.call();
                }));
            }
            try {
                assertTrue(listos.await(10, TimeUnit.SECONDS));
            } finally {
                inicio.countDown();
            }
            List<T> resultados = new ArrayList<>();
            for (Future<T> pendiente : pendientes) {
                resultados.add(pendiente.get(20, TimeUnit.SECONDS));
            }
            return resultados;
        }
    }
}
