package org.deliverygo.repository;

import org.deliverygo.model.Rol;
import org.deliverygo.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Long> {

    public Optional<Rol> findByNombre(String nombre);

}
