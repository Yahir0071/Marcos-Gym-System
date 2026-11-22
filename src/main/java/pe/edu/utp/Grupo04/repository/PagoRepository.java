package pe.edu.utp.Grupo04.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.utp.Grupo04.model.Pago;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findByMembresiaId(Long membresiaId);

    Optional<Pago> findByReferencia(String referencia);

    boolean existsByMembresiaId(Long membresiaId);

    @EntityGraph(attributePaths = {"membresia.plan", "membresia.socio"})
    @Query("""
            select p from Pago p
            where :texto = '' or lower(p.referencia) = :texto or p.membresia.socio.dni = :texto
            or lower(concat(p.membresia.socio.nombres, ' ', p.membresia.socio.apellidos)) like concat('%', :texto, '%')
            """)
    Page<Pago> listarEnCaja(@Param("texto") String texto, Pageable pageable);

    @Query("SELECT COALESCE(SUM(p.monto), 0.0) FROM Pago p WHERE p.fechaPago >= :inicio AND p.fechaPago < :fin")
    Double sumarMontoEnPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    long countByFechaPagoGreaterThanEqualAndFechaPagoLessThan(LocalDateTime inicio, LocalDateTime fin);
}
