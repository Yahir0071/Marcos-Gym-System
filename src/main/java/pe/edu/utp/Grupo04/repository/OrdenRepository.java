package pe.edu.utp.Grupo04.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.utp.Grupo04.model.Orden;

import java.util.List;
import java.util.Collection;
import java.util.Optional;

public interface OrdenRepository extends JpaRepository<Orden, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Orden> findLockedById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Orden> findLockedByIdAndSocioId(Long id, Long socioId);

    boolean existsByReservaIdAndEstadoPagoIn(Long reservaId, Collection<String> estados);


    @EntityGraph(attributePaths = "detalles")
    Optional<Orden> findByReferencia(String referencia);

    @EntityGraph(attributePaths = "detalles")
    Optional<Orden> findByIdAndSocioId(Long id, Long socioId);

    @EntityGraph(attributePaths = "detalles")
    List<Orden> findBySocioIdOrderByFechaDesc(Long socioId);

    @Query("""
            select o from Orden o
            where o.estadoPago in ('PAGADO', 'EN_PROCESO')
              and (:filtro = '' or locate(:filtro, lower(o.compradorEmail)) > 0
                or locate(:filtro, lower(o.compradorDocNum)) > 0
                or locate(:filtro, lower(o.compradorNombre)) > 0)
            order by o.fecha desc, o.id desc
            """)
    Page<Orden> buscarHistorialPorCliente(@Param("filtro") String filtro, Pageable pageable);

    @Query("select coalesce(max(o.id), 0) from Orden o")
    Long maxId();
}
