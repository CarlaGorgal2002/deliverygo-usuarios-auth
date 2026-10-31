package ar.edu.uade.api.deliverygo.usuarios.repository;

import ar.edu.uade.api.deliverygo.usuarios.domain.Direccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DireccionRepository extends JpaRepository<Direccion, Long> {

    List<Direccion> findByUsuarioIdAndActivaTrueOrderByIdAsc(Long usuarioId);

    // Una dirección inexistente, dada de baja o de otro usuario es indistinguible (RN-05).
    Optional<Direccion> findByIdAndUsuarioIdAndActivaTrue(Long id, Long usuarioId);

    List<Direccion> findByUsuarioIdAndPredeterminadaTrue(Long usuarioId);
}
