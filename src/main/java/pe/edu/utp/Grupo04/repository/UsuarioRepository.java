package pe.edu.utp.Grupo04.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;
import pe.edu.utp.Grupo04.model.Usuario;
import java.util.Optional;
import java.util.Collection;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsername(String username);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Usuario> findForUpdateByUsername(String username);
    Slice<Usuario> findByRolInOrderByUsernameAsc(Collection<String> roles, Pageable page);

    boolean existsByRolAndActivoTrue(String rol);
}
