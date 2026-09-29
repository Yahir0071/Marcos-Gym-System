package pe.edu.utp.Grupo04.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.utp.Grupo04.model.RegistroAfluencia;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RegistroAfluenciaRepository extends JpaRepository<RegistroAfluencia, Long> {
    List<RegistroAfluencia> findByFecha(LocalDate fecha);
}