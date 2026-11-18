package pe.edu.utp.Grupo04.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pe.edu.utp.Grupo04.model.Orden;

import java.util.List;
import java.util.Optional;

public interface OrdenRepository extends JpaRepository<Orden, Long> {

    @EntityGraph(attributePaths = "detalles")
    Optional<Orden> findByReferencia(String referencia);

    @EntityGraph(attributePaths = "detalles")
    Optional<Orden> findByIdAndSocioId(Long id, Long socioId);

    @EntityGraph(attributePaths = "detalles")
    List<Orden> findBySocioIdOrderByFechaDesc(Long socioId);

    @EntityGraph(attributePaths = "detalles")
    List<Orden> findAllByOrderByFechaDesc();

    @Query("select coalesce(max(o.id), 0) from Orden o")
    Long maxId();
}