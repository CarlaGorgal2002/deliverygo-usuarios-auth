package org.deliverygo.security;

import org.deliverygo.model.EstadoUsuario;
import org.deliverygo.model.Usuario;
import org.deliverygo.repository.UsuarioRepository;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

import java.util.List;

@Configuration
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtFilter;
    private final UsuarioRepository usuarioRepository;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter, UsuarioRepository usuarioRepository) {
        this.jwtFilter = jwtFilter;
        this.usuarioRepository = usuarioRepository;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()).cors(cors -> {
                }).headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**", "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/h2-console/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/v1/auth/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/v1/auth/**").hasAnyRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/v1/auth/**").hasAnyRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService users(UsuarioRepository usuarioRepository) {
        return email -> {
            Usuario usuario = usuarioRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

            return User.withUsername(usuario.getEmail())
                    .password(usuario.getPasswordHash())
                    .roles(usuario.getRol().getNombre())   // agrega "ROLE_" solo → ROLE_ADMIN, ROLE_CLIENTE...
                    .disabled(usuario.getEstado() == EstadoUsuario.INACTIVO)
                    .build();
        };
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(List.of("http://localhost:5173"));
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", c);
        return source;
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration c) throws Exception {
        return c.getAuthenticationManager();
    }
}
