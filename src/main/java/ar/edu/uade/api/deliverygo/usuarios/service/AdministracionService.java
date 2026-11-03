package ar.edu.uade.api.deliverygo.usuarios.service;

import ar.edu.uade.api.deliverygo.usuarios.domain.EstadoUsuario;
import ar.edu.uade.api.deliverygo.usuarios.domain.Rol;
import ar.edu.uade.api.deliverygo.usuarios.domain.Usuario;
import ar.edu.uade.api.deliverygo.usuarios.dto.UsuarioResponse;
import ar.edu.uade.api.deliverygo.usuarios.exception.AccesoProhibidoException;
import ar.edu.uade.api.deliverygo.usuarios.exception.SesionInvalidaException;
import ar.edu.uade.api.deliverygo.usuarios.exception.UsuarioInactivoException;
import ar.edu.uade.api.deliverygo.usuarios.exception.UsuarioNoEncontradoException;
import ar.edu.uade.api.deliverygo.usuarios.mapper.UsuarioMapper;
import ar.edu.uade.api.deliverygo.usuarios.repository.RolRepository;
import ar.edu.uade.api.deliverygo.usuarios.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

// Operaciones de administración sobre cuentas ajenas: CU-06 (estado) y CU-07 (rol).
@Service
public class AdministracionService {

    private static final Logger log = LoggerFactory.getLogger(AdministracionService.class);

    static final String ROL_ADMIN = "ADMIN";
    static final String MSG_SOLO_ADMIN = "Sólo un administrador puede realizar esta operación";
    static final String MSG_PROPIA_CUENTA = "Un administrador no puede modificar su propia cuenta";
    static final String MSG_SESION = "La autenticación falta, es inválida o está vencida";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final UsuarioMapper usuarioMapper;

    public AdministracionService(UsuarioRepository usuarioRepository, RolRepository rolRepository,
                                 UsuarioMapper usuarioMapper) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.usuarioMapper = usuarioMapper;
    }

    // SRV-10 (CU-06). Idempotente: pedir el estado que la cuenta ya tiene devuelve 200 sin cambios.
    @Transactional
    public UsuarioResponse cambiarEstado(Long administradorId, Long usuarioId, EstadoUsuario nuevoEstado) {
        Usuario usuario = bloquearYAutorizar(administradorId, usuarioId);
        if (usuario.getEstado() != nuevoEstado) {
            usuario.setEstado(nuevoEstado);
            log.info("CU-06: el administrador {} dejó la cuenta {} en estado {}", administradorId, usuarioId, nuevoEstado);
        }
        return usuarioMapper.toResponse(usuarioRepository.saveAndFlush(usuario));
    }

    // SRV-11 (CU-07). Idempotente. Los tokens ya emitidos conservan el rol anterior hasta vencer (DM-03):
    // quien necesite el rol vigente lo consulta con CU-09 o CU-10.
    @Transactional
    public UsuarioResponse cambiarRol(Long administradorId, Long usuarioId, String nuevoRol) {
        Usuario usuario = bloquearYAutorizar(administradorId, usuarioId);
        if (!usuario.getRol().getNombre().equals(nuevoRol)) {
            Rol rol = rolRepository.findByNombre(nuevoRol)
                    .orElseThrow(() -> new IllegalStateException("El rol " + nuevoRol + " no está cargado"));
            usuario.setRol(rol);
            log.info("CU-07: el administrador {} asignó el rol {} a la cuenta {}", administradorId, nuevoRol, usuarioId);
        }
        return usuarioMapper.toResponse(usuarioRepository.saveAndFlush(usuario));
    }

    // RN-11 y RN-12. El administrador se vuelve a leer de la base: el rol del token puede estar vencido
    // (por ejemplo, lo degradaron después de iniciar sesión). Las dos filas se bloquean en orden de id para que
    // dos administradores que se modifican entre sí al mismo tiempo no se dejen sin ninguno, ni se bloqueen.
    private Usuario bloquearYAutorizar(Long administradorId, Long usuarioId) {
        Usuario administrador = null;
        Usuario objetivo = null;
        for (Long id : new TreeSet<>(List.of(administradorId, usuarioId))) {
            Optional<Usuario> bloqueado = usuarioRepository.findByIdForUpdate(id);
            if (id.equals(administradorId)) {
                administrador = bloqueado.orElse(null);
            }
            if (id.equals(usuarioId)) {
                objetivo = bloqueado.orElse(null);
            }
        }

        if (administrador == null) {
            throw new SesionInvalidaException(MSG_SESION);
        }
        if (!administrador.estaActivo()) {
            throw new UsuarioInactivoException(UsuarioService.MSG_INACTIVO);
        }
        if (!ROL_ADMIN.equals(administrador.getRol().getNombre())) {
            throw new AccesoProhibidoException(MSG_SOLO_ADMIN);
        }
        if (administradorId.equals(usuarioId)) {
            throw new AccesoProhibidoException(MSG_PROPIA_CUENTA);
        }
        if (objetivo == null) {
            throw new UsuarioNoEncontradoException(UsuarioService.MSG_NO_ENCONTRADO);
        }
        return objetivo;
    }
}
