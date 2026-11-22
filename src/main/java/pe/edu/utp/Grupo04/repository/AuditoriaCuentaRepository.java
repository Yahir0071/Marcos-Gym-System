package pe.edu.utp.Grupo04.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.utp.Grupo04.model.AuditoriaCuenta;

import java.util.List;

@Repository
public interface AuditoriaCuentaRepository extends JpaRepository<AuditoriaCuenta, Long> {
    List<AuditoriaCuenta> findTop50ByOrderByFechaHoraDescIdDesc();
}
