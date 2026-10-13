package org.deliverygo.repository;

import org.deliverygo.model.Direccion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DireccionRepository extends JpaRepository<Direccion, Long> {

    List<Direccion> findByUsuarioIdAndActivaTrue(Long usuarioId);

    Optional<Direccion> findByIdAndUsuarioIdAndActivaTrue(Long id, Long usuarioId);

    Optional<Direccion> findByUsuarioIdAndPredeterminadaTrue(Long usuarioId);
}
