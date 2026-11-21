package pe.edu.utp.Grupo04.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import pe.edu.utp.Grupo04.model.Reserva;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Reserva> findLockedById(Long id);

    List<Reserva> findBySocioIdOrderByFechaReservaDesc(Long socioId);
    List<Reserva> findBySocioUsuarioUsernameOrderByFechaReservaDesc(String username);
    long countByEstado(pe.edu.utp.Grupo04.model.enums.EstadoReserva estado);
}
