package ar.edu.uade.api.deliverygo.usuarios.repository;

import ar.edu.uade.api.deliverygo.usuarios.domain.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// REP-01 a REP-03: findByEmail y existsBy... son consultas derivadas; findById y save los provee JpaRepository.
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    // RN-02 al modificar el perfil: el email nuevo no puede pertenecer a otro usuario.
    boolean existsByEmailAndIdNot(String email, Long id);
}
