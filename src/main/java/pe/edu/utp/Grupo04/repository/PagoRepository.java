package pe.edu.utp.Grupo04.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.utp.Grupo04.model.Pago;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findByMembresiaId(Long membresiaId);

    @Query("SELECT COALESCE(SUM(p.monto), 0.0) FROM Pago p WHERE p.fechaPago >= :inicio AND p.fechaPago < :fin")
    Double sumarMontoEnPeriodo(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    long countByFechaPagoGreaterThanEqualAndFechaPagoLessThan(LocalDateTime inicio, LocalDateTime fin);
}