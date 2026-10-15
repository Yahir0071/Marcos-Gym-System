package pe.edu.utp.Grupo04.service;

import pe.edu.utp.Grupo04.model.Plan;

import java.util.List;
import java.util.Optional;

public interface PlanService {
    List<Plan> listarActivos();
    List<Plan> listarTodos();
    Optional<Plan> buscarPorId(Long id);
    Plan guardar(Plan plan);
    void cambiarEstado(Long id, boolean activo);
}
