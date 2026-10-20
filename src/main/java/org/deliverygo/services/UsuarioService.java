package org.deliverygo.services;

import org.deliverygo.dto.ModificarPerfilRequest;
import org.deliverygo.dto.RegistrarUsuarioRequest;
import org.deliverygo.dto.UsuarioResponse;
import org.deliverygo.exceptions.EmailDuplicadoException;
import org.deliverygo.exceptions.RecursoNoEncontradoException;
import org.deliverygo.exceptions.UsuarioInactivoException;
import org.deliverygo.model.EstadoUsuario;
import org.deliverygo.model.Rol;
import org.deliverygo.model.Usuario;
import org.deliverygo.repository.RolRepository;
import org.deliverygo.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    private static final String Rol_Inicial = "CLIENTE";

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

        if (usuario.getPassword() == null || usuario.getPassword().length() < 8) {
            throw new IllegalArgumentException("La contraseña no puede ser nula o vacía. Debe tener al menos 8 caracteres. ");
        }

    }

    private Usuario buscarUsuario(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    @Transactional
    public UsuarioResponse registrarUsuario (RegistrarUsuarioRequest usuario) {

        validarUsuario(usuario);

        String email = usuario.getEmail().trim().toLowerCase();

        if (usuarioRepository.existsByEmail(email)) {
            throw new EmailDuplicadoException("Ya existe un usuario con ese email");
        }

            Rol rol = rolRepository.findByNombre(Rol_Inicial)
                    .orElseThrow(() -> new IllegalStateException("El rol " + Rol_Inicial + " no esta cargado."));

            if ("ADMIN".equals(rol.getNombre())) {
                throw new IllegalArgumentException("No se puede registrar un usuario ADMIN");
            }

            String telefono = (usuario.getTelefono() == null || usuario.getTelefono().isBlank())
                    ? null
                    : usuario.getTelefono().trim();

            Usuario nuevoUsuario = new Usuario(
                    usuario.getNombre().trim(),
                    usuario.getApellido().trim(),
                    email,
                    passwordEncoder.encode(usuario.getPassword()),
                    telefono,
                    rol
            );
            try {
                return toDto(usuarioRepository.saveAndFlush(nuevoUsuario));
            } catch (DataIntegrityViolationException e) {
                // dos registros simultáneos con el mismo email: gana la restricción UNIQUE
                throw new EmailDuplicadoException("Ya existe un usuario con ese email");
            }

        }

    // SRV-03 (CU-03)
    @Transactional(readOnly = true)
    public UsuarioResponse consultarPerfil(Long usuarioId) {

        Usuario usuario = buscarUsuario(usuarioId);

        // RN-04
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new UsuarioInactivoException("Cuenta inactiva");
        }

        return toDto(usuario);
    }

    // SRV-04 (CU-04)
    @Transactional
    public UsuarioResponse modificarPerfil(Long usuarioId, ModificarPerfilRequest request) {

        Usuario usuario = buscarUsuario(usuarioId);

        // RN-04
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new UsuarioInactivoException("Cuenta inactiva");
        }

        // RN-01: lo que viene informado no puede estar vacío
        if (request.getNombre() != null && request.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (request.getApellido() != null && request.getApellido().isBlank()) {
            throw new IllegalArgumentException("El apellido no puede estar vacío");
        }

        // RN-02: si cambia el email, no puede ser de otro usuario
        if (request.getEmail() != null) {
            String nuevoEmail = request.getEmail().trim().toLowerCase();

            if (!nuevoEmail.matches(EMAIL_REGEX)) {
                throw new IllegalArgumentException("El email no tiene un formato válido");
            }

            if (!nuevoEmail.equals(usuario.getEmail())) {
                usuarioRepository.findByEmail(nuevoEmail)
                        .filter(otro -> !otro.getId().equals(usuario.getId()))
                        .ifPresent(otro -> {
                            throw new EmailDuplicadoException("Ya existe un usuario con ese email");
                        });
                usuario.setEmail(nuevoEmail);
            }
        }

        // RN-10: solo nombre, apellido, teléfono y email. Rol, estado y contraseña no se tocan.
        if (request.getNombre() != null) {
            usuario.setNombre(request.getNombre().trim());
        }
        if (request.getApellido() != null) {
            usuario.setApellido(request.getApellido().trim());
        }
        if (request.getTelefono() != null) {
            usuario.setTelefono(request.getTelefono().isBlank() ? null : request.getTelefono().trim());
        }

        try {
            return toDto(usuarioRepository.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException e) {
            throw new EmailDuplicadoException("Ya existe un usuario con ese email");
        }
    }

    }


