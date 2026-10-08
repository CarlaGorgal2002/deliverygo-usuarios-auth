package org.deliverygo.repository;

import org.deliverygo.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    public Usuario findByNombre(String nombre);

    public List<Usuario> findByNombreContainingIgnoreCase(String nombre);

    public Usuario findByEmail(String email);

    public List<Usuario> findByEmailContainingIgnoreCase(String email);

    public boolean existsByEmail(String email);

}
