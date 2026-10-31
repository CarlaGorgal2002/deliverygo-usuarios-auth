package ar.edu.uade.api.deliverygo.usuarios.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_BEARER = "bearerAuth";
    private static final String ESQUEMA_API_KEY = "moduleApiKey";

    @Bean
    public OpenAPI usuariosOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("DeliveryGo - Usuarios y Autenticación - Grupo 02")
                        .version("1.1.0")
                        .description("""
                                Registro, inicio de sesión con JWT, perfil y direcciones de entrega (CU-01 a CU-05) y los casos de evolución:
                                desactivar usuarios (CU-06), roles (CU-07), recuperar contraseña (CU-08), renovar y validar sesión (CU-09)
                                y consulta de estado y rol para otros módulos (CU-10).
                                Rutas bajo /api/v1, JSON UTF-8, errores normalizados y X-Correlation-Id en cada respuesta.
                                El contrato objetivo completo está en docs/openapi/deliverygo-usuarios.yaml.""")
                        .contact(new Contact().name("Grupo 02 - Usuarios y Autenticación")))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_BEARER, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT de POST /api/v1/auth/login. En Swagger UI pegar sólo el token."))
                        .addSecuritySchemes(ESQUEMA_API_KEY, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name("X-Api-Key")
                                .description("Credencial propia de un módulo consumidor (Pedidos, Logística, Catálogo, Notificaciones, "
                                        + "Pagos, Opiniones y Analítica). La entrega el equipo de Usuarios; sólo lectura.")))
                // Seguridad por defecto; registro y login la anulan con @SecurityRequirements.
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BEARER));
    }
}
