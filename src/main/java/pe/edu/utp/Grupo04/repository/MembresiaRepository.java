package pe.edu.utp.Grupo04.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.utp.Grupo04.model.Membresia;
import pe.edu.utp.Grupo04.model.enums.EstadoMembresia;
import java.util.List;

@Repository
public interface MembresiaRepository extends JpaRepository<Membresia, Long> {
    List<Membresia> findBySocioId(Long socioId);
    List<Membresia> findByEstado(EstadoMembresia estado);
}