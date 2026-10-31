package ar.edu.uade.api.deliverygo.usuarios.config;

import ar.edu.uade.api.deliverygo.usuarios.domain.Rol;
import ar.edu.uade.api.deliverygo.usuarios.repository.RolRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

// Carga los roles que necesita el módulo. Sólo agrega los que faltan, así es seguro con cualquier base.
@Configuration
public class DemoDataLoader {

    private static final Map<String, String> ROLES = Map.of(
            "CLIENTE", "Usuario que realiza pedidos",
            "COMERCIO", "Usuario que administra un comercio",
            "REPARTIDOR", "Usuario que entrega pedidos",
            "ADMIN", "Administrador del sistema");

    @Bean
    CommandLineRunner cargarRoles(RolRepository rolRepository) {
        return args -> ROLES.forEach((nombre, descripcion) -> {
            if (rolRepository.findByNombre(nombre).isEmpty()) {
                rolRepository.save(new Rol(nombre, descripcion));
            }
        });
    }
}
