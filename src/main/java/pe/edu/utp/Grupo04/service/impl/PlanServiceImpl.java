package pe.edu.utp.Grupo04.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo04.exception.RecursoNoEncontradoException;
import pe.edu.utp.Grupo04.model.Plan;
import pe.edu.utp.Grupo04.repository.PlanRepository;
import pe.edu.utp.Grupo04.service.PlanService;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class PlanServiceImpl implements PlanService {

    private final PlanRepository planRepository;

    public PlanServiceImpl(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Plan> listarActivos() {
        return planRepository.findByActivoTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Plan> listarTodos() {
        return planRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Plan> buscarPorId(Long id) {
        if (id == null) return Optional.empty();
        return planRepository.findById(id);
    }

    @Override
    public Plan guardar(Plan plan) {
        return planRepository.save(plan);
    }

    @Override
    public void cambiarEstado(Long id, boolean activo) {
        Plan plan = planRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Plan no encontrado con ID: " + id));
        plan.setActivo(activo);
        planRepository.save(plan);
    }
}
