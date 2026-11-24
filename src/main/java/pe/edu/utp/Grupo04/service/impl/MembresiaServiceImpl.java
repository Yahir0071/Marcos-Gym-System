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
import pe.edu.utp.Grupo04.service.TarifaInscripcion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Locale;
import java.util.UUID;
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
        LocalDate hoy = LocalDate.now(TarifaInscripcion.ZONA_LIMA);
        actualizarVigencias(membresias, hoy);

        // Retornar la primera membresía activa más reciente
        return membresias.stream()
                .filter(m -> m.getEstado() == EstadoMembresia.ACTIVA && m.getFechaFin() != null && !m.getFechaFin().isBefore(hoy))
                .max(Comparator.comparing(Membresia::getFechaFin));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Membresia> buscarPorId(Long id) {
        if (id == null) return Optional.empty();
        return membresiaRepository.findById(id);
    }

    @Override
    public Membresia crearPreinscripcion(Socio socio, Plan plan, TarifaInscripcion tarifa) {
        LocalDateTime ahora = LocalDateTime.now(TarifaInscripcion.ZONA_LIMA);
        int dias = plan.getDuracionDias() != null && plan.getDuracionDias() > 0 ? plan.getDuracionDias() : 30;
        Membresia membresia = new Membresia();
        membresia.setSocio(socio);
        membresia.setPlan(plan);
        membresia.setEstado(EstadoMembresia.PENDIENTE);
        membresia.setFechaInicio(ahora.toLocalDate());
        membresia.setFechaFin(tarifa.fechaFin(ahora.toLocalDate(), dias));
        membresia.setCodigoPreinscripcion("INS-" + UUID.randomUUID().toString().toUpperCase(Locale.ROOT));
        membresia.setPreinscripcionVence(ahora.plusHours(72));
        membresia.setMontoReservado(tarifa.total());
        membresia.setPeriodoReservado(tarifa.periodo());
        return membresiaRepository.save(membresia);
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

        LocalDate hoy = LocalDate.now(TarifaInscripcion.ZONA_LIMA);
        LocalDate inicio = (fechaInicio != null) ? fechaInicio : hoy;

        // C09: Considerar renovación sobre membresía activa existente sin perder vigencia
        List<Membresia> existentes = membresiaRepository.findBySocioId(socioId);
        Optional<LocalDate> ultimaVigencia = existentes.stream()
                .filter(m -> m.getEstado() == EstadoMembresia.ACTIVA || esProgramada(m))
                .map(Membresia::getFechaFin)
                .filter(fin -> fin != null && !fin.isBefore(hoy))
                .max(LocalDate::compareTo);
        if (ultimaVigencia.isPresent() && !ultimaVigencia.get().isBefore(inicio)) {
            inicio = ultimaVigencia.get().plusDays(1);
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
        LocalDate hoy = LocalDate.now(TarifaInscripcion.ZONA_LIMA);
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
        LocalDate hoy = LocalDate.now(TarifaInscripcion.ZONA_LIMA);
        actualizarVigencias(membresiaRepository.findByEstado(EstadoMembresia.ACTIVA), hoy);
        actualizarVigencias(membresiaRepository.findByEstado(EstadoMembresia.PENDIENTE), hoy);
    }

    private void actualizarVigencias(List<Membresia> membresias, LocalDate hoy) {
        for (Membresia m : membresias) {
            boolean programada = esProgramada(m);
            if (m.getFechaFin() == null || (!programada && m.getEstado() != EstadoMembresia.ACTIVA)) {
                continue;
            }
            if (m.getFechaFin().isBefore(hoy)) {
                m.setEstado(EstadoMembresia.VENCIDA);
                membresiaRepository.save(m);
            } else if (programada && m.getFechaInicio() != null && !m.getFechaInicio().isAfter(hoy)) {
                m.setEstado(EstadoMembresia.ACTIVA);
                membresiaRepository.save(m);
            }
        }
    }

    private boolean esProgramada(Membresia membresia) {
        return membresia.getEstado() == EstadoMembresia.PENDIENTE && membresia.getCodigoPreinscripcion() == null;
    }
}
