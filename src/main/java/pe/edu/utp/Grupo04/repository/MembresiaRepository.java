package pe.edu.utp.Grupo04.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.utp.Grupo04.model.Membresia;
import pe.edu.utp.Grupo04.model.enums.EstadoMembresia;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MembresiaRepository extends JpaRepository<Membresia, Long> {
    List<Membresia> findBySocioId(Long socioId);
    List<Membresia> findByEstado(EstadoMembresia estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Membresia m where m.id = :id")
    Optional<Membresia> buscarParaActualizar(@Param("id") Long id);

    @EntityGraph(attributePaths = {"socio", "plan"})
    @Query("""
            select m from Membresia m
            where m.estado = :estado and m.preinscripcionVence > :ahora
            and (:referencia = '' or upper(m.codigoPreinscripcion) = :referencia or m.socio.dni = :referencia)
            order by m.preinscripcionVence, m.id
            """)
    List<Membresia> buscarPreinscripcionesPorCobrar(@Param("estado") EstadoMembresia estado,
            @Param("ahora") LocalDateTime ahora, @Param("referencia") String referencia, Pageable pageable);
}
