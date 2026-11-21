package pe.edu.utp.Grupo04.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo04.exception.RecursoNoEncontradoException;
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
                .filter(m -> m.getEstado() == EstadoMembresia.ACTIVA && m.getFechaFin() != null && !m.getFechaFin().isBefore(hoy))
                .findFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Membresia> buscarPorId(Long id) {
        if (id == null) return Optional.empty();
        return membresiaRepository.findById(id);
    }

    @Override
    public Membresia asignarPlanASocio(Long socioId, Long planId, LocalDate fechaInicio) {
        return asignarPlanASocio(socioId, planId, fechaInicio, "MENSUAL", 1);
    }

    @Override
    public Membresia asignarPlanASocio(Long socioId, Long planId, LocalDate fechaInicio, String periodo, Integer meses) {
        Socio socio = socioRepository.findById(socioId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Socio no encontrado con ID: " + socioId));

        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Plan no encontrado con ID: " + planId));

        LocalDate hoy = LocalDate.now();
        LocalDate inicio = (fechaInicio != null) ? fechaInicio : hoy;

        // C09: Considerar renovación sobre membresía activa existente sin perder vigencia
        List<Membresia> existentes = membresiaRepository.findBySocioId(socioId);
        Optional<Membresia> activaOpt = existentes.stream()
                .filter(m -> m.getEstado() == EstadoMembresia.ACTIVA && m.getFechaFin() != null && !m.getFechaFin().isBefore(hoy))
                .findFirst();

        if (activaOpt.isPresent()) {
            LocalDate finActual = activaOpt.get().getFechaFin();
            if (finActual.isAfter(inicio) || finActual.isEqual(inicio)) {
                inicio = finActual.plusDays(1);
            }
        }

        // C09: Cálculo de vigencia contratada (ANUAL = 12 meses / 1 año)
        LocalDate fin;
        if ("ANUAL".equalsIgnoreCase(periodo) || (meses != null && meses >= 12)) {
            fin = inicio.plusYears(1);
        } else if ("SEMESTRAL".equalsIgnoreCase(periodo) || (meses != null && meses == 6)) {
            fin = inicio.plusMonths(6);
        } else if ("TRIMESTRAL".equalsIgnoreCase(periodo) || (meses != null && meses == 3)) {
            fin = inicio.plusMonths(3);
        } else if (meses != null && meses > 1) {
            fin = inicio.plusMonths(meses);
        } else {
            int duracion = (plan.getDuracionDias() != null && plan.getDuracionDias() > 0) ? plan.getDuracionDias() : 30;
            fin = inicio.plusDays(duracion);
        }

        Membresia membresia = new Membresia();
        membresia.setSocio(socio);
        membresia.setPlan(plan);
        membresia.setFechaInicio(inicio);
        membresia.setFechaFin(fin);

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
