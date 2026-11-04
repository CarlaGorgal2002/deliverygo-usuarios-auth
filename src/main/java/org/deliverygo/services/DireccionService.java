package org.deliverygo.services;

import org.deliverygo.dto.CrearDireccionRequest;
import org.deliverygo.dto.DireccionResponse;
import org.deliverygo.dto.ModificarDireccionRequest;
import org.deliverygo.exceptions.RecursoNoEncontradoException;
import org.deliverygo.exceptions.UsuarioInactivoException;
import org.deliverygo.mapper.DireccionMapper;
import org.deliverygo.model.Direccion;
import org.deliverygo.model.EstadoUsuario;
import org.deliverygo.model.Usuario;
import org.deliverygo.repository.DireccionRepository;
import org.deliverygo.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DireccionService {
    private final DireccionRepository direccionRepository;
    private final UsuarioRepository usuarioRepository;

    public DireccionService(DireccionRepository direccionRepository, UsuarioRepository usuarioRepository) {
        this.direccionRepository = direccionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    // RN-04: el usuario existe y está ACTIVO
    private Usuario buscarUsuarioActivo(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
        if (usuario.getEstado() != EstadoUsuario.ACTIVO) {
            throw new UsuarioInactivoException("Cuenta inactiva");
        }
        return usuario;
    }

    // REP-06 / RN-05 / A1: existe, está activa y es de ESE usuario.
    // Si no cumple, siempre 404: no se revela si la dirección existe.
    private Direccion buscarDireccionPropia(Long direccionId, Long usuarioId) {
        return direccionRepository.findByIdAndUsuarioIdAndActivaTrue(direccionId, usuarioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Dirección no encontrada"));
    }

    // REP-08 / RN-07: baja la marca de la predeterminada actual (si hay)
    private void desmarcarPredeterminadaActual(Long usuarioId) {
        direccionRepository.findByUsuarioIdAndPredeterminadaTrue(usuarioId)
                .ifPresent(actual -> actual.setPredeterminada(false));
    }

    // RN-01 / A2: informa TODOS los campos a corregir, no solo el primero
    private void validarDatos(String calle, String numero, String ciudad,
                              String provincia, String codigoPostal) {
        List<String> faltantes = new ArrayList<>();
        if (esVacio(calle)) faltantes.add("calle");
        if (esVacio(numero)) faltantes.add("numero");
        if (esVacio(ciudad)) faltantes.add("ciudad");
        if (esVacio(provincia)) faltantes.add("provincia");
        if (esVacio(codigoPostal)) faltantes.add("codigoPostal");

        if (!faltantes.isEmpty()) {
            throw new IllegalArgumentException(
                    "Datos obligatorios faltantes o vacíos: " + String.join(", ", faltantes));
        }
    }

    private boolean esVacio(String s) {
        return s == null || s.isBlank();
    }

    // para los campos opcionales (piso, departamento, referencia)
    private String limpiar(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    // ================= SRV-05: alta =================

    @Transactional
    public DireccionResponse crearDireccion(Long usuarioId, CrearDireccionRequest request) {

        Usuario usuario = buscarUsuarioActivo(usuarioId);                       // RN-04
        validarDatos(request.getCalle(), request.getNumero(), request.getCiudad(),
                request.getProvincia(), request.getCodigoPostal());             // RN-01

        // RN-07: queda predeterminada si es la primera o si el cliente lo pide
        boolean esLaPrimera = direccionRepository.findByUsuarioIdAndActivaTrue(usuarioId).isEmpty();
        boolean predeterminada = esLaPrimera || Boolean.TRUE.equals(request.getPredeterminada());

        if (predeterminada) {
            desmarcarPredeterminadaActual(usuarioId);
        }

        Direccion nueva = new Direccion(
                request.getCalle().trim(),
                request.getNumero().trim(),
                limpiar(request.getPiso()),
                limpiar(request.getDepartamento()),
                request.getCiudad().trim(),
                request.getProvincia().trim(),
                request.getCodigoPostal().trim(),
                limpiar(request.getReferencia()),
                predeterminada,
                true,
                usuario
        );

        return DireccionMapper.toResponse(direccionRepository.save(nueva));
    }

    // ================= SRV-06: modificación =================

    @Transactional
    public DireccionResponse modificarDireccion(Long usuarioId, Long direccionId,
                                                ModificarDireccionRequest request) {

        buscarUsuarioActivo(usuarioId);                                         // RN-04
        Direccion direccion = buscarDireccionPropia(direccionId, usuarioId);    // RN-05
        validarDatos(request.getCalle(), request.getNumero(), request.getCiudad(),
                request.getProvincia(), request.getCodigoPostal());             // RN-01

        direccion.setCalle(request.getCalle().trim());
        direccion.setNumero(request.getNumero().trim());
        direccion.setPiso(limpiar(request.getPiso()));
        direccion.setDepartamento(limpiar(request.getDepartamento()));
        direccion.setCiudad(request.getCiudad().trim());
        direccion.setProvincia(request.getProvincia().trim());
        direccion.setCodigoPostal(request.getCodigoPostal().trim());
        direccion.setReferencia(limpiar(request.getReferencia()));
        // RN-07: acá NO se toca predeterminada (el DTO ni siquiera tiene el campo)

        return DireccionMapper.toResponse(direccionRepository.save(direccion));
    }

    // ================= SRV-07: baja lógica =================

    @Transactional
    public void eliminarDireccion(Long usuarioId, Long direccionId) {

        buscarUsuarioActivo(usuarioId);                                         // RN-04
        Direccion direccion = buscarDireccionPropia(direccionId, usuarioId);    // RN-05

        // DM-04: no se borra la fila; Pedidos puede tener referenciada esta dirección
        direccion.setActiva(false);
        direccion.setPredeterminada(false);   // A3: si era la predeterminada, el usuario queda sin ninguna
        direccionRepository.save(direccion);
    }

    // ================= SRV-08: seleccionar predeterminada =================

    @Transactional
    public DireccionResponse seleccionarDireccionPredeterminada(Long usuarioId, Long direccionId) {

        buscarUsuarioActivo(usuarioId);                                         // RN-04
        Direccion direccion = buscarDireccionPropia(direccionId, usuarioId);    // RN-05

        if (!direccion.isPredeterminada()) {
            desmarcarPredeterminadaActual(usuarioId);   // RN-07: las dos escrituras van en la
            direccion.setPredeterminada(true);          // misma transacción (DM-10)
            direccionRepository.save(direccion);
        }
        return DireccionMapper.toResponse(direccion);
    }

    // ================= SRV-09: listado =================

    @Transactional(readOnly = true)
    public List<DireccionResponse> listarDirecciones(Long usuarioId) {

        buscarUsuarioActivo(usuarioId);                                         // RN-04

        return direccionRepository.findByUsuarioIdAndActivaTrue(usuarioId).stream()
                .map(DireccionMapper::toResponse)
                .toList();
    }

}
