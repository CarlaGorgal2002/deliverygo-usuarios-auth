package ar.edu.uade.api.deliverygo.usuarios;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// DM-07: el primer ADMIN se aprovisiona por configuración. Usa su propia base en memoria para no
// compartir tablas con el resto de las pruebas.
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:administrador-inicial",
        "app.bootstrap-admin.email=  Admin.Inicial@Example.com ",
        "app.bootstrap-admin.password=AdminInicial2026!"})
class AdministradorInicialTest extends ApiTestSupport {

    @Test
    void elAdministradorConfiguradoPuedeIniciarSesionYAdministrar() throws Exception {
        login("admin.inicial@example.com", "AdminInicial2026!")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMIN"));
        login("admin.inicial@example.com", PASSWORD).andExpect(status().isUnauthorized());
    }
}
