package org.deliverygo.config;

import org.deliverygo.model.Rol;
import org.deliverygo.repository.RolRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.*;


@Configuration
public class DemoDataLoader {

    @Bean
    CommandLineRunner demo(RolRepository rolRepository) {
        return args -> {
            if (rolRepository.count() == 0) {
                rolRepository.save(new Rol("CLIENTE", "Usuario que realiza pedidos"));
                rolRepository.save(new Rol("COMERCIO", "Usuario que administra un comercio"));
                rolRepository.save(new Rol("REPARTIDOR", "Usuario que entrega pedidos"));
                rolRepository.save(new Rol("ADMIN", "Administrador del sistema"));
            }
        };
    }
}
