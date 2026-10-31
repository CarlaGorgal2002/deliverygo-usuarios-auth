package ar.edu.uade.api.deliverygo.usuarios.service;

import ar.edu.uade.api.deliverygo.usuarios.domain.Direccion;
import ar.edu.uade.api.deliverygo.usuarios.domain.Usuario;
import ar.edu.uade.api.deliverygo.usuarios.dto.CrearDireccionRequest;
import ar.edu.uade.api.deliverygo.usuarios.dto.DireccionResponse;
import ar.edu.uade.api.deliverygo.usuarios.dto.ModificarDireccionRequest;
import ar.edu.uade.api.deliverygo.usuarios.exception.DireccionNoEncontradaException;
import ar.edu.uade.api.deliverygo.usuarios.exception.UsuarioInactivoException;
import ar.edu.uade.api.deliverygo.usuarios.exception.UsuarioNoEncontradoException;
import ar.edu.uade.api.deliverygo.usuarios.mapper.DireccionMapper;
import ar.edu.uade.api.deliverygo.usuarios.repository.DireccionRepository;
import ar.edu.uade.api.deliverygo.usuarios.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

// CU-05. Toda escritura toma primero un lock sobre la fila del usuario: así dos pedidos simultáneos
// sobre sus direcciones se ejecutan de a uno y nunca quedan dos predeterminadas (RN-07).
@Service
public class DireccionService {

    private static final String MSG_DIRECCION_NO_ENCONTRADA = "Dirección no encontrada";

    private final DireccionRepository direccionRepository;
    private final UsuarioRepository usuarioRepository;
    private final DireccionMapper direccionMapper;

    public DireccionService(DireccionRepository direccionRepository, UsuarioRepository usuarioRepository,
                            DireccionMapper direccionMapper) {
        this.direccionRepository = direccionRepository;
        this.usuarioRepository = usuarioRepository;
        this.direccionMapper = direccionMapper;
    }

    // SRV-09 (API-05)
    @Transactional(readOnly = true)
    public List<DireccionResponse> listarDirecciones(Long usuarioId) {
        verificarTitularActivo(usuarioRepository.findById(usuarioId));
        return direccionRepository.findByUsuarioIdAndActivaTrueOrderByIdAsc(usuarioId).stream()
                .map(direccionMapper::toResponse)
                .toList();
    }

    // SRV-05 (API-06)
    @Transactional
    public DireccionResponse crearDireccion(Long usuarioId, CrearDireccionRequest request) {
        Usuario usuario = bloquearTitularActivo(usuarioId);

        Direccion direccion = new Direccion(usuario,
                request.calle().trim(),
                request.numero().trim(),
                Textos.recortarONulo(request.piso()),
                Textos.recortarONulo(request.departamento()),
                request.ciudad().trim(),
                request.provincia().trim(),
                request.codigoPostal().trim());

        // RN-07: por omisión no es predeterminada, ni siquiera la primera.
        if (Boolean.TRUE.equals(request.predeterminada())) {
            desmarcarPredeterminadas(usuarioId);
            direccion.setPredeterminada(true);
        }
        return direccionMapper.toResponse(direccionRepository.save(direccion));
    }

    // SRV-06 (API-07). PUT completo de los datos editables; no toca predeterminada.
    @Transactional
    public DireccionResponse modificarDireccion(Long usuarioId, Long direccionId, ModificarDireccionRequest request) {
        bloquearTitularActivo(usuarioId);
        Direccion direccion = buscarDireccionActiva(usuarioId, direccionId);

        direccion.actualizarDatos(
                request.calle().trim(),
                request.numero().trim(),
                Textos.recortarONulo(request.piso()),
                Textos.recortarONulo(request.departamento()),
                request.ciudad().trim(),
                request.provincia().trim(),
                request.codigoPostal().trim());
        return direccionMapper.toResponse(direccion);
    }

    // SRV-07 (API-08). Baja lógica: la fila se conserva inactiva para mantener referencias históricas.
    @Transactional
    public void eliminarDireccion(Long usuarioId, Long direccionId) {
        bloquearTitularActivo(usuarioId);
        Direccion direccion = buscarDireccionActiva(usuarioId, direccionId);

        // RN-07: si era la predeterminada se desmarca y no se elige otra automáticamente.
        direccion.setActiva(false);
        direccion.setPredeterminada(false);
    }

    // SRV-08 (API-09). Idempotente: seleccionar la que ya es predeterminada no cambia nada.
    @Transactional
    public DireccionResponse seleccionarPredeterminada(Long usuarioId, Long direccionId) {
        bloquearTitularActivo(usuarioId);
        Direccion direccion = buscarDireccionActiva(usuarioId, direccionId);

        if (!direccion.isPredeterminada()) {
            desmarcarPredeterminadas(usuarioId);
            direccion.setPredeterminada(true);
        }
        return direccionMapper.toResponse(direccion);
    }

    // RN-05: la identidad del solicitante ya se verificó; acá sólo se comprueba RN-04.
    private Usuario bloquearTitularActivo(Long usuarioId) {
        return verificarTitularActivo(usuarioRepository.findByIdForUpdate(usuarioId));
    }

    private Usuario verificarTitularActivo(Optional<Usuario> usuario) {
        Usuario titular = usuario.orElseThrow(() -> new UsuarioNoEncontradoException(UsuarioService.MSG_NO_ENCONTRADO));
        if (!titular.estaActivo()) {
            throw new UsuarioInactivoException(UsuarioService.MSG_INACTIVO);
        }
        return titular;
    }

    // Inexistente, dada de baja o de otro usuario: el mismo 404, sin distinguir la causa (RN-05).
    private Direccion buscarDireccionActiva(Long usuarioId, Long direccionId) {
        return direccionRepository.findByIdAndUsuarioIdAndActivaTrue(direccionId, usuarioId)
                .orElseThrow(() -> new DireccionNoEncontradaException(MSG_DIRECCION_NO_ENCONTRADA));
    }

    private void desmarcarPredeterminadas(Long usuarioId) {
        direccionRepository.findByUsuarioIdAndPredeterminadaTrue(usuarioId)
                .forEach(direccion -> direccion.setPredeterminada(false));
    }
}
