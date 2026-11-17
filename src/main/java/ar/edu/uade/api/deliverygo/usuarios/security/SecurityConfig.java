package ar.edu.uade.api.deliverygo.usuarios.security;

import ar.edu.uade.api.deliverygo.usuarios.exception.ErrorResponseWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    // Roles de las personas (tabla roles). Los módulos consumidores no mandan credencial: sólo leen (RN-15).
    private static final String[] PERSONAS = {"CLIENTE", "COMERCIO", "REPARTIDOR", "ADMIN"};
    private static final String ADMIN = "ADMIN";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
                                            ErrorResponseWriter errorWriter) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // La consola de H2 se arma con frames: se permiten sólo desde el mismo origen
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                // 401 y 403 con el mismo DTO-08 que el resto de la API (por defecto Spring respondería vacío)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, ex) -> errorWriter.escribir(
                                request, response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED",
                                "La autenticación falta, es inválida o está vencida"))
                        .accessDeniedHandler((request, response, ex) -> errorWriter.escribir(
                                request, response, HttpStatus.FORBIDDEN, "FORBIDDEN",
                                "No tiene permiso para acceder a este recurso")))
                .authorizeHttpRequests(auth -> auth
                        // Públicas: registro, login y recuperación de contraseña (CU-01, CU-02, CU-08)
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login",
                                "/api/v1/auth/password/forgot", "/api/v1/auth/password/reset").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        // Estado del servicio, público
                        .requestMatchers(HttpMethod.GET, "/api/v1/status").permitAll()
                        // Consola de H2 (sólo desarrollo): tiene su propio login con el usuario de la base
                        .requestMatchers(PathRequest.toH2Console()).permitAll()

                        // CU-09: renovar y validar la sesión es sólo para personas
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").hasAnyRole(PERSONAS)
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/session").hasAnyRole(PERSONAS)

                        // CU-06 y CU-07: sólo ADMIN (el service lo vuelve a verificar contra la base, RN-11)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/*/status", "/api/v1/users/*/role").hasRole(ADMIN)

                        // CU-03, CU-05 y CU-10: lecturas que usan los otros módulos, sin credencial (RN-15). Con el JWT
                        // de una persona (el front) el controller exige además que sea el titular (RN-05).
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/*", "/api/v1/users/*/addresses",
                                "/api/v1/users/*/access").permitAll()

                        // CU-04 y CU-05: modificar perfil y direcciones, sólo personas (el titular lo controla el controller)
                        .requestMatchers("/api/v1/users/**").hasAnyRole(PERSONAS)

                        .anyRequest().authenticated())
                .addFilterBefore(new JwtAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") List<String> origenes) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origenes);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", ApiRequestFilter.CORRELATION_HEADER));
        config.setExposedHeaders(List.of(ApiRequestFilter.CORRELATION_HEADER, "Location"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
