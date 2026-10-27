package pe.edu.utp.Grupo04.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.Grupo04.model.RutinaDia;
import java.util.List;
public interface RutinaDiaRepository extends JpaRepository<RutinaDia, Long> {
    List<RutinaDia> findByUsernameOrderByVigenteDesdeAscDiaAsc(String username);
}
