package pe.edu.utp.Grupo04.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;
import pe.edu.utp.Grupo04.model.SolicitudCongelamiento;
import pe.edu.utp.Grupo04.model.enums.EstadoSolicitudCongelamiento;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitudCongelamientoRepository extends JpaRepository<SolicitudCongelamiento, Long> {
    @EntityGraph(attributePaths = "membresia.plan")
    List<SolicitudCongelamiento> findBySocioIdOrderByFechaSolicitudDesc(Long socioId);
    boolean existsBySocioIdAndEstado(Long socioId, EstadoSolicitudCongelamiento estado);

    @EntityGraph(attributePaths = {"socio", "membresia.plan"})
    Slice<SolicitudCongelamiento> findByEstadoOrderByFechaSolicitudDescIdDesc(
            EstadoSolicitudCongelamiento estado, Pageable pagina);

    @Query("select s.socio.id as socioId, s.membresia.id as membresiaId from SolicitudCongelamiento s where s.id = :id")
    Optional<Titular> buscarTitular(Long id);

    interface Titular {
        Long getSocioId();
        Long getMembresiaId();
    }

    List<SolicitudCongelamiento> findByMembresiaIdAndEstadoAndFechaInicioSolicitadaLessThanEqual(
            Long membresiaId, EstadoSolicitudCongelamiento estado, LocalDate ultimaFecha);
}
