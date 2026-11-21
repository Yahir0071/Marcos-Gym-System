package pe.edu.utp.Grupo04.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import pe.edu.utp.Grupo04.model.Socio;
import java.util.Optional;

@Repository
public interface SocioRepository extends JpaRepository<Socio, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Socio> findLockedById(Long id);

    Optional<Socio> findByDni(String dni);
    Optional<Socio> findByUsuarioUsername(String username);
}
