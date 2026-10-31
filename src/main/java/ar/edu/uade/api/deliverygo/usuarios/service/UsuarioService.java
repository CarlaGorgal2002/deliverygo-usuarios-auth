package ar.edu.uade.api.deliverygo.usuarios.service;

import ar.edu.uade.api.deliverygo.usuarios.domain.Rol;
import ar.edu.uade.api.deliverygo.usuarios.domain.Usuario;
import ar.edu.uade.api.deliverygo.usuarios.dto.AccesoUsuarioResponse;
import ar.edu.uade.api.deliverygo.usuarios.dto.ModificarPerfilRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.RegistrarUsuarioRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.UsuarioResponse;
import ar.edu.uade.api.deliverygo.usuarios.exception.DatoInvalidoException;
import ar.edu.uade.api.deliverygo.usuarios.exception.EmailDuplicadoException;
import ar.edu.uade.api.deliverygo.usuarios.exception.UsuarioInactivoException;
import ar.edu.uade.api.deliverygo.usuarios.exception.UsuarioNoEncontradoException;
import ar.edu.uade.api.deliverygo.usuarios.mapper.UsuarioMapper;
import ar.edu.uade.api.deliverygo.usuarios.repository.RolRepository;
import ar.edu.uade.api.deliverygo.usuarios.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Service
public class UsuarioService {

    // RN-06: el registro público siempre crea CLIENTE; otros roles se aprovisionan aparte (CU-07).
    static final String ROL_INICIAL = "CLIENTE";
    static final String MSG_EMAIL_DUPLICADO = "El email ya está registrado";
    static final String MSG_INACTIVO = "La cuenta está inactiva";
    static final String MSG_NO_ENCONTRADO = "Usuario no encontrado";

    // BCrypt sólo considera los primeros 72 bytes; rechazarlos evita que dos claves distintas valgan lo mismo.
    private static final int MAX_BYTES_PASSWORD = 72;

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioMapper usuarioMapper;

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                          PasswordEncoder passwordEncoder, UsuarioMapper usuarioMapper) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.usuarioMapper = usuarioMapper;
    }

    // SRV-01 (CU-01)
    @Transactional
    public UsuarioResponse registrarUsuario(RegistrarUsuarioRequest request) {
        if (request.password().getBytes(StandardCharsets.UTF_8).length > MAX_BYTES_PASSWORD) {
            throw new DatoInvalidoException("password", "No puede superar " + MAX_BYTES_PASSWORD + " bytes");
        }

        // RN-02 / DC-02
        String email = Textos.normalizarEmail(request.email());
        if (usuarioRepository.existsByEmail(email)) {
            throw new EmailDuplicadoException(MSG_EMAIL_DUPLICADO);
        }

        Rol rol = rolRepository.findByNombre(ROL_INICIAL)
                .orElseThrow(() -> new IllegalStateException("El rol " + ROL_INICIAL + " no está cargado"));

        // RN-03: sólo se persiste el hash
        Usuario usuario = new Usuario(
                request.nombre().trim(),
                request.apellido().trim(),
                email,
                passwordEncoder.encode(request.password()),
                Textos.recortarONulo(request.telefono()),
                rol);

        try {
            return usuarioMapper.toResponse(usuarioRepository.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException e) {
            // Los demás límites ya los validó el DTO: lo que queda es la restricción UNIQUE del email,
            // que gana cuando dos registros simultáneos pasan el existsByEmail a la vez (RN-02).
            throw new EmailDuplicadoException(MSG_EMAIL_DUPLICADO);
        }
    }

    // SRV-03 (CU-03)
    @Transactional(readOnly = true)
    public UsuarioResponse consultarPerfil(Long usuarioId) {
        return usuarioMapper.toResponse(buscarTitularActivo(usuarioId));
    }

    // SRV-04 (CU-04)
    @Transactional
    public UsuarioResponse modificarPerfil(Long usuarioId, ModificarPerfilRequest request) {
        Usuario usuario = buscarTitularActivo(usuarioId);

        // RN-02: el email nuevo no puede pertenecer a otro usuario. Se valida antes de modificar nada.
        String email = Textos.normalizarEmail(request.email());
        if (usuarioRepository.existsByEmailAndIdNot(email, usuario.getId())) {
            throw new EmailDuplicadoException(MSG_EMAIL_DUPLICADO);
        }

        // RN-10 / DC-03: PUT completo de nombre, apellido, email y teléfono. Rol, estado y contraseña no se tocan.
        usuario.setNombre(request.nombre().trim());
        usuario.setApellido(request.apellido().trim());
        usuario.setEmail(email);
        usuario.setTelefono(Textos.recortarONulo(request.telefono()));

        try {
            return usuarioMapper.toResponse(usuarioRepository.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException e) {
            throw new EmailDuplicadoException(MSG_EMAIL_DUPLICADO);
        }
    }

    // SRV-03 para módulos consumidores (CU-03, RN-15). El módulo recibe el estado y decide con sus reglas:
    // una cuenta INACTIVA se devuelve igual, con 200.
    @Transactional(readOnly = true)
    public UsuarioResponse consultarPerfilParaModulo(Long usuarioId) {
        return usuarioMapper.toResponse(buscarExistente(usuarioId));
    }

    // SRV-16 (CU-10). Sólo estado y rol, sin datos personales.
    @Transactional(readOnly = true)
    public AccesoUsuarioResponse consultarAcceso(Long usuarioId) {
        Usuario usuario = buscarExistente(usuarioId);
        return new AccesoUsuarioResponse(usuario.getId(), usuario.getEstado(), usuario.getRol().getNombre());
    }

    private Usuario buscarExistente(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new UsuarioNoEncontradoException(MSG_NO_ENCONTRADO));
    }

    // RN-04: sólo un titular ACTIVO opera sobre su perfil; la identidad (RN-05) ya la verificó @PreAuthorize.
    private Usuario buscarTitularActivo(Long usuarioId) {
        Usuario usuario = buscarExistente(usuarioId);
        if (!usuario.estaActivo()) {
            throw new UsuarioInactivoException(MSG_INACTIVO);
        }
        return usuario;
    }
}
