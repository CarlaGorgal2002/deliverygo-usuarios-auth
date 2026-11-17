package ar.edu.uade.api.deliverygo.usuarios.config;

import ar.edu.uade.api.deliverygo.usuarios.domain.Rol;
import ar.edu.uade.api.deliverygo.usuarios.domain.Usuario;
import ar.edu.uade.api.deliverygo.usuarios.repository.RolRepository;
import ar.edu.uade.api.deliverygo.usuarios.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;
import java.util.Map;

// Carga los datos iniciales del módulo. Sólo agrega lo que falta, así es seguro con cualquier base.
@Configuration
public class DemoDataLoader {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private static final Map<String, String> ROLES = Map.of(
            "CLIENTE", "Usuario que realiza pedidos",
            "COMERCIO", "Usuario que administra un comercio",
            "REPARTIDOR", "Usuario que entrega pedidos",
            "ADMIN", "Administrador del sistema");

    @Bean
    @Order(1)
    CommandLineRunner cargarRoles(RolRepository rolRepository) {
        return args -> ROLES.forEach((nombre, descripcion) -> {
            if (rolRepository.findByNombre(nombre).isEmpty()) {
                rolRepository.save(new Rol(nombre, descripcion));
            }
        });
    }

    // DM-07: los roles distintos de CLIENTE no se autoasignan; el primer ADMIN se aprovisiona por configuración
    // (ADMIN_EMAIL y ADMIN_PASSWORD) y desde ahí administra los demás con CU-07. Sin esas variables no se crea
    // ninguno: no hay un administrador ni una contraseña por defecto.
    @Bean
    @Order(2)
    CommandLineRunner cargarAdministradorInicial(RolRepository rolRepository, UsuarioRepository usuarioRepository,
                                                 PasswordEncoder passwordEncoder,
                                                 @Value("${app.bootstrap-admin.email:}") String email,
                                                 @Value("${app.bootstrap-admin.password:}") String password) {
        return args -> {
            if (email.isBlank() && password.isBlank()) {
                return;
            }
            if (email.isBlank() || password.length() < 8 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
                throw new IllegalStateException(
                        "ADMIN_EMAIL y ADMIN_PASSWORD deben definirse juntos; la contraseña debe tener entre 8 caracteres y 72 bytes");
            }
            String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
            if (usuarioRepository.existsByEmail(emailNormalizado)) {
                return;
            }
            Rol admin = rolRepository.findByNombre("ADMIN").orElseThrow();
            usuarioRepository.save(new Usuario("Administrador", "DeliveryGo", emailNormalizado,
                    passwordEncoder.encode(password), null, admin));
            log.info("Administrador inicial creado");
        };
    }

    // Usuario de prueba para demos locales: un CLIENTE común, igual al que crea el registro (CU-01), ACTIVO y sin
    // direcciones. Se desactiva con DEMO_USER_ENABLED=false; las pruebas automáticas no lo cargan.
    static final String EMAIL_USUARIO_DEMO = "cliente@deliverygo.test";
    static final String PASSWORD_USUARIO_DEMO = "ClienteDemo2026!";

    @Bean
    @Order(3)
    CommandLineRunner cargarUsuarioDemo(RolRepository rolRepository, UsuarioRepository usuarioRepository,
                                        PasswordEncoder passwordEncoder,
                                        @Value("${app.demo-user.enabled:false}") boolean habilitado) {
        return args -> {
            if (!habilitado || usuarioRepository.existsByEmail(EMAIL_USUARIO_DEMO)) {
                return;
            }
            Rol cliente = rolRepository.findByNombre("CLIENTE").orElseThrow();
            usuarioRepository.save(new Usuario("Cliente", "Demo", EMAIL_USUARIO_DEMO,
                    passwordEncoder.encode(PASSWORD_USUARIO_DEMO), "1122334455", cliente));
            log.info("Usuario de prueba creado: {} (rol CLIENTE)", EMAIL_USUARIO_DEMO);
        };
    }
}
