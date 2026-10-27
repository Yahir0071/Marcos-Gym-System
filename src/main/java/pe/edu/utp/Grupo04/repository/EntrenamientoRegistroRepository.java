package pe.edu.utp.Grupo04.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.Grupo04.model.EntrenamientoRegistro;
import java.util.List;
import java.time.LocalDate;
import java.util.Optional;
public interface EntrenamientoRegistroRepository extends JpaRepository<EntrenamientoRegistro, Long> {
    List<EntrenamientoRegistro> findByUsername(String username);
    Optional<EntrenamientoRegistro> findByUsernameAndFecha(String username, LocalDate fecha);
}
