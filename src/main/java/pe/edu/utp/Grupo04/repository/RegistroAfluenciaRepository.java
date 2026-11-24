package pe.edu.utp.Grupo04.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pe.edu.utp.Grupo04.model.RegistroAfluencia;
import pe.edu.utp.Grupo04.model.enums.FranjaHoraria;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegistroAfluenciaRepository extends JpaRepository<RegistroAfluencia, Long> {
    List<RegistroAfluencia> findByFecha(LocalDate fecha);
    List<RegistroAfluencia> findByFechaBetweenOrderByFechaAsc(LocalDate inicio, LocalDate fin);
    List<RegistroAfluencia> findAllByOrderByFechaDescFechaRegistroDesc();
    Optional<RegistroAfluencia> findByFechaAndFranjaHoraria(LocalDate fecha, FranjaHoraria franjaHoraria);
    Optional<RegistroAfluencia> findTopByOrderByFechaRegistroDesc();

    @Modifying
    @Query(value = """
            INSERT INTO registros_afluencia (fecha, franja_horaria, concurrencia_aprox, fecha_registro)
            VALUES (:fecha, :franja, :concurrencia, :actualizado)
            ON CONFLICT (fecha, franja_horaria) DO UPDATE
            SET concurrencia_aprox = EXCLUDED.concurrencia_aprox,
                fecha_registro = EXCLUDED.fecha_registro
            """, nativeQuery = true)
    void registrarOActualizar(LocalDate fecha, String franja, Integer concurrencia, LocalDateTime actualizado);

    @Query("SELECT MAX(r.fecha) FROM RegistroAfluencia r")
    Optional<LocalDate> findUltimaFechaRegistrada();
}
