package pe.edu.utp.Grupo04.service;

import pe.edu.utp.Grupo04.model.Membresia;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MembresiaService {
    List<Membresia> listarTodas();
    List<Membresia> listarPorSocioId(Long socioId);
    Optional<Membresia> obtenerMembresiaActivaPorUsername(String username);
    Optional<Membresia> buscarPorId(Long id);
    Membresia asignarPlanASocio(Long socioId, Long planId, LocalDate fechaInicio);
    long calcularDiasRestantes(Membresia membresia);
    boolean estaPorVencer(Membresia membresia, int diasUmbral);
    List<Membresia> listarProximasAVencer(int diasUmbral);
    long contarMembresiasActivas();
    void sincronizarEstadosVencimiento();
}
