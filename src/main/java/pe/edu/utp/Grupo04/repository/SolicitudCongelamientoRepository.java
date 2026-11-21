package pe.edu.utp.Grupo04.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.utp.Grupo04.model.SolicitudCongelamiento;
import pe.edu.utp.Grupo04.model.enums.EstadoSolicitudCongelamiento;

import java.util.List;

@Repository
public interface SolicitudCongelamientoRepository extends JpaRepository<SolicitudCongelamiento, Long> {
    List<SolicitudCongelamiento> findBySocioIdOrderByFechaSolicitudDesc(Long socioId);
    boolean existsBySocioIdAndEstado(Long socioId, EstadoSolicitudCongelamiento estado);
}
