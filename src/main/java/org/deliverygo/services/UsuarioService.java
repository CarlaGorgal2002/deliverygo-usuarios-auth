package org.deliverygo.services;

import jakarta.transaction.Transactional;
import org.deliverygo.dto.RegistrarUsuarioRequest;
import org.deliverygo.dto.UsuarioResponse;
import org.deliverygo.exceptions.EmailDuplicadoException;
import org.deliverygo.model.Rol;
import org.deliverygo.model.Usuario;
import org.deliverygo.repository.RolRepository;
import org.deliverygo.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public static UsuarioResponse toDto(Usuario u) {
        return new UsuarioResponse(
                u.getId(),
                u.getNombre(),
                u.getApellido(),
                u.getEmail(),
                u.getTelefono(),
                u.getEstado(),
                u.getFechaAlta(),
                u.getRol() != null ? u.getRol().getNombre() : null
        );
    }

    private void validarUsuario(RegistrarUsuarioRequest usuario) {

        if (usuario.getNombre() == null || usuario.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre no puede ser nulo o vacío");
        }
        if (usuario.getApellido() == null || usuario.getApellido().isBlank()) {
            throw new IllegalArgumentException("El apellido no puede ser nulo o vacío");
        }
        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            throw new IllegalArgumentException("El email no puede ser nulo o vacío");
        }
        if(!usuario.getEmail().toLowerCase().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new IllegalArgumentException("El email no tiene un formato válido");
        }
        if(usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new EmailDuplicadoException("Ya existe un usuario con ese email");
        }
        if(usuario.getRolId() == null) {
            throw new IllegalArgumentException("El rol no puede ser nulo");
        }
        if (usuario.getPassword() == null || usuario.getPassword().isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede ser nula o vacía");
        }
        if (usuario.getTelefono() == null || usuario.getTelefono().isBlank()) {
            throw new IllegalArgumentException("El teléfono no puede ser nulo o vacío");
        }
    }

    @Transactional
    public UsuarioResponse crearUsuario (RegistrarUsuarioRequest usuario) {

        validarUsuario(usuario);

        Rol rol = rolRepository.findById(usuario.getRolId())
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado"));

        if ("ADMIN".equals(rol.getNombre())) {
            throw new IllegalArgumentException("No se puede registrar un usuario ADMIN");
        }

        Usuario nuevoUsuario = new Usuario(
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                passwordEncoder.encode(usuario.getPassword()),
                usuario.getTelefono(),
                rol
        );

        Usuario usuarioGuardado = usuarioRepository.save(nuevoUsuario);

        return toDto(usuarioGuardado);
    }

}
