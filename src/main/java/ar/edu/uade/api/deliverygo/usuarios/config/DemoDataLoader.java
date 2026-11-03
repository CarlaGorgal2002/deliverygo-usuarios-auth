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
}
