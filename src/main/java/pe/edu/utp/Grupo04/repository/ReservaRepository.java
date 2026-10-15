package pe.edu.utp.Grupo04.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.utp.Grupo04.model.Reserva;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findBySocioIdOrderByFechaReservaDesc(Long socioId);
    List<Reserva> findBySocioUsuarioUsernameOrderByFechaReservaDesc(String username);
    long countByEstado(pe.edu.utp.Grupo04.model.enums.EstadoReserva estado);
}