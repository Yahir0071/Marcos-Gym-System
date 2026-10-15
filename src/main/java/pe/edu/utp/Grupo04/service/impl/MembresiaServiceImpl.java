package pe.edu.utp.Grupo04.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo04.model.Membresia;
import pe.edu.utp.Grupo04.model.Plan;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.model.enums.EstadoMembresia;
import pe.edu.utp.Grupo04.repository.MembresiaRepository;
import pe.edu.utp.Grupo04.repository.PlanRepository;
import pe.edu.utp.Grupo04.repository.SocioRepository;
import pe.edu.utp.Grupo04.service.MembresiaService;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class MembresiaServiceImpl implements MembresiaService {

    private final MembresiaRepository membresiaRepository;
    private final SocioRepository socioRepository;
    private final PlanRepository planRepository;

    public MembresiaServiceImpl(MembresiaRepository membresiaRepository,
                                SocioRepository socioRepository,
                                PlanRepository planRepository) {
        this.membresiaRepository = membresiaRepository;
        this.socioRepository = socioRepository;
        this.planRepository = planRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Membresia> listarTodas() {
        return membresiaRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Membresia> listarPorSocioId(Long socioId) {
        return membresiaRepository.findBySocioId(socioId);
    }

    @Override
    public Optional<Membresia> obtenerMembresiaActivaPorUsername(String username) {
        Optional<Socio> socioOpt = socioRepository.findByUsuarioUsername(username);
        if (socioOpt.isEmpty()) {
            return Optional.empty();
        }

        List<Membresia> membresias = membresiaRepository.findBySocioId(socioOpt.get().getId());
        LocalDate hoy = LocalDate.now();

        // Actualizar estados si alguna fecha venció
        for (Membresia m : membresias) {
            if (m.getFechaFin() != null && m.getFechaFin().isBefore(hoy) && m.getEstado() == EstadoMembresia.ACTIVA) {
                m.setEstado(EstadoMembresia.VENCIDA);
                membresiaRepository.save(m);
            }
        }

        // Retornar la primera membresía activa más reciente
        return membresias.stream()
                .filter(m -> m.getEstado() == EstadoMembresia.ACTIVA)
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Membresia> buscarPorId(Long id) {
        return membresiaRepository.findById(id);
    }

    @Override
    public Membresia asignarPlanASocio(Long socioId, Long planId, LocalDate fechaInicio) {
        Socio socio = socioRepository.findById(socioId)
                .orElseThrow(() -> new IllegalArgumentException("Socio no encontrado con ID: " + socioId));

        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("Plan no encontrado con ID: " + planId));

        LocalDate inicio = (fechaInicio != null) ? fechaInicio : LocalDate.now();
        LocalDate fin = inicio.plusDays(plan.getDuracionDias());

        Membresia membresia = new Membresia();
        membresia.setSocio(socio);
        membresia.setPlan(plan);
        membresia.setFechaInicio(inicio);
        membresia.setFechaFin(fin);

        LocalDate hoy = LocalDate.now();
        if (fin.isBefore(hoy)) {
            membresia.setEstado(EstadoMembresia.VENCIDA);
        } else if (inicio.isAfter(hoy)) {
            membresia.setEstado(EstadoMembresia.PENDIENTE);
        } else {
            membresia.setEstado(EstadoMembresia.ACTIVA);
        }

        return membresiaRepository.save(membresia);
    }

    @Override
    public long calcularDiasRestantes(Membresia membresia) {
        if (membresia == null || membresia.getFechaFin() == null) {
            return 0;
        }
        LocalDate hoy = LocalDate.now();
        if (membresia.getFechaFin().isBefore(hoy)) {
            return 0;
        }
        return ChronoUnit.DAYS.between(hoy, membresia.getFechaFin());
    }

    @Override
    public boolean estaPorVencer(Membresia membresia, int diasUmbral) {
        if (membresia == null || membresia.getEstado() != EstadoMembresia.ACTIVA) {
            return false;
        }
        long dias = calcularDiasRestantes(membresia);
        return dias > 0 && dias <= diasUmbral;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Membresia> listarProximasAVencer(int diasUmbral) {
        return membresiaRepository.findByEstado(EstadoMembresia.ACTIVA).stream()
                .filter(m -> estaPorVencer(m, diasUmbral))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long contarMembresiasActivas() {
        return membresiaRepository.findByEstado(EstadoMembresia.ACTIVA).size();
    }

    @Override
    public void sincronizarEstadosVencimiento() {
        LocalDate hoy = LocalDate.now();
        List<Membresia> activas = membresiaRepository.findByEstado(EstadoMembresia.ACTIVA);
        for (Membresia m : activas) {
            if (m.getFechaFin() != null && m.getFechaFin().isBefore(hoy)) {
                m.setEstado(EstadoMembresia.VENCIDA);
                membresiaRepository.save(m);
            }
        }
    }
}
