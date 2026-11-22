package pe.edu.utp.Grupo04.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.utp.Grupo04.model.EstadoNotificacion;

import java.util.Collection;
import java.util.List;

public interface EstadoNotificacionRepository extends JpaRepository<EstadoNotificacion, Long> {
    List<EstadoNotificacion> findByUsuarioIdAndAvisoClaveIn(Long usuarioId, Collection<String> claves);
}
