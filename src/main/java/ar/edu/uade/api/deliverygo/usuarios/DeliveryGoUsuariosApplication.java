package ar.edu.uade.api.deliverygo.usuarios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// Las credenciales se validan en AuthService contra la base del módulo (RN-08 exige verificar la
// contraseña antes de informar el estado), por eso no se usa el usuario en memoria de Spring Boot.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class DeliveryGoUsuariosApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeliveryGoUsuariosApplication.class, args);
    }
}
