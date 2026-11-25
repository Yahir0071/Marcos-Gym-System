package pe.edu.utp.Grupo04.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pe.edu.utp.Grupo04.model.RegistroAcceso;

import java.util.Optional;

@Repository
public interface RegistroAccesoRepository extends JpaRepository<RegistroAcceso, Long> {

    Optional<RegistroAcceso> findByQrToken(String qrToken);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RegistroAcceso r WHERE r.qrToken = :qrToken")
    Optional<RegistroAcceso> findLockedByQrToken(String qrToken);

    Optional<RegistroAcceso> findTopBySocioIdAndEstadoOrderByFechaUsoDesc(Long socioId, String estado);
}
