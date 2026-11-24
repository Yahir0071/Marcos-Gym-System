package pe.edu.utp.Grupo04.service.impl;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo04.dto.SolicitudCongelamientoDTO;
import pe.edu.utp.Grupo04.model.Membresia;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.model.SolicitudCongelamiento;
import pe.edu.utp.Grupo04.model.enums.EstadoMembresia;
import pe.edu.utp.Grupo04.model.enums.EstadoSolicitudCongelamiento;
import pe.edu.utp.Grupo04.repository.SolicitudCongelamientoRepository;
import pe.edu.utp.Grupo04.repository.MembresiaRepository;
import pe.edu.utp.Grupo04.repository.SocioRepository;
import pe.edu.utp.Grupo04.service.CongelamientoService;
import pe.edu.utp.Grupo04.service.MembresiaService;
import pe.edu.utp.Grupo04.service.SocioService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
@Transactional
public class CongelamientoServiceImpl implements CongelamientoService {

    private static final ZoneId ZONA_GIMNASIO = ZoneId.of("America/Lima");
    private final SolicitudCongelamientoRepository congelamientoRepository;
    private final MembresiaRepository membresiaRepository;
    private final SocioRepository socioRepository;
    private final SocioService socioService;
    private final MembresiaService membresiaService;

    public CongelamientoServiceImpl(SolicitudCongelamientoRepository congelamientoRepository,
                                    MembresiaRepository membresiaRepository,
                                    SocioRepository socioRepository,
                                    SocioService socioService,
                                    MembresiaService membresiaService) {
        this.congelamientoRepository = congelamientoRepository;
        this.membresiaRepository = membresiaRepository;
        this.socioRepository = socioRepository;
        this.socioService = socioService;
        this.membresiaService = membresiaService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SolicitudCongelamiento> listarPorUsername(String username) {
        return socioService.buscarPorUsername(username)
                .map(socio -> congelamientoRepository.findBySocioIdOrderByFechaSolicitudDesc(socio.getId()))
                .orElse(Collections.emptyList());
    }

    @Override
    public SolicitudCongelamiento registrarSolicitud(String username, SolicitudCongelamientoDTO dto) {
        Socio socio = socioService.buscarPorUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró un perfil de socio asociado a la sesión."));
        socioRepository.findLockedById(socio.getId())
                .orElseThrow(() -> new IllegalStateException("No se encontró el perfil del socio."));

        Membresia membresiaActiva = membresiaService.obtenerMembresiaActivaPorUsername(username)
                .orElseThrow(() -> new IllegalStateException("Solo los socios con membresía activa pueden solicitar congelamiento."));

        if (dto.getFechaInicioSolicitada() == null || dto.getFechaInicioSolicitada().isBefore(LocalDate.now(ZONA_GIMNASIO))) {
            throw new IllegalArgumentException("La fecha de inicio de congelamiento debe ser hoy o posterior.");
        }

        if (membresiaActiva.getFechaFin() != null && dto.getFechaInicioSolicitada().isAfter(membresiaActiva.getFechaFin())) {
            throw new IllegalArgumentException("La fecha de inicio debe estar dentro de la vigencia actual de tu membresía.");
        }

        if (congelamientoRepository.existsBySocioIdAndEstado(socio.getId(), EstadoSolicitudCongelamiento.PENDIENTE)) {
            throw new IllegalStateException("Ya tienes una solicitud de congelamiento pendiente de revisión.");
        }

        SolicitudCongelamiento solicitud = new SolicitudCongelamiento();
        solicitud.setSocio(socio);
        solicitud.setMembresia(membresiaActiva);
        solicitud.setDiasSolicitados(dto.getDias());
        solicitud.setFechaInicioSolicitada(dto.getFechaInicioSolicitada());
        solicitud.setMotivo(dto.getMotivo().trim());
        solicitud.setEstado(EstadoSolicitudCongelamiento.PENDIENTE);
        solicitud.setFechaSolicitud(LocalDateTime.now(ZONA_GIMNASIO));

        return congelamientoRepository.save(solicitud);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<SolicitudCongelamiento> listarParaRevision(EstadoSolicitudCongelamiento estado, int pagina) {
        return congelamientoRepository.findByEstadoOrderByFechaSolicitudDescIdDesc(
                estado, PageRequest.of(pagina, 25));
    }

    @Override
    public void resolver(Long id, EstadoSolicitudCongelamiento decision, String operador) {
        if (decision != EstadoSolicitudCongelamiento.APROBADA && decision != EstadoSolicitudCongelamiento.RECHAZADA) {
            throw new IllegalArgumentException("Selecciona aprobar o rechazar la solicitud.");
        }
        var titular = congelamientoRepository.buscarTitular(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la solicitud."));
        socioRepository.findLockedById(titular.getSocioId())
                .orElseThrow(() -> new IllegalStateException("No se encontró el perfil del socio."));
        Membresia membresia = membresiaRepository.buscarParaActualizar(titular.getMembresiaId())
                .orElseThrow(() -> new IllegalStateException("No se encontró la membresía asociada."));
        SolicitudCongelamiento solicitud = congelamientoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontró la solicitud."));
        if (solicitud.getEstado() != EstadoSolicitudCongelamiento.PENDIENTE) {
            throw new IllegalStateException("Esta solicitud ya fue atendida.");
        }
        if (decision == EstadoSolicitudCongelamiento.APROBADA) {
            aprobar(solicitud, membresia);
        }
        solicitud.setEstado(decision);
        solicitud.setFechaResolucion(LocalDateTime.now(ZONA_GIMNASIO));
        solicitud.setResueltoPor(operador);
    }

    private void aprobar(SolicitudCongelamiento solicitud, Membresia membresia) {
        LocalDate inicio = solicitud.getFechaInicioSolicitada();
        LocalDate fin = ultimaFecha(solicitud);
        if (membresia.getEstado() != EstadoMembresia.ACTIVA || membresia.getFechaInicio() == null ||
                membresia.getFechaFin() == null || inicio.isBefore(membresia.getFechaInicio()) ||
                inicio.isAfter(membresia.getFechaFin()) || inicio.isBefore(LocalDate.now(ZONA_GIMNASIO))) {
            throw new IllegalStateException("La fecha solicitada ya no está disponible. Rechaza la solicitud para que el socio pueda enviar otra.");
        }
        if (hayCruce(membresia.getId(), inicio, fin)) {
            throw new IllegalStateException("La membresía ya tiene una pausa aprobada en esas fechas.");
        }
        membresia.setFechaFin(membresia.getFechaFin().plusDays(solicitud.getDiasSolicitados()));
        moverRenovaciones(membresia, solicitud.getSocio().getId());
    }

    private void moverRenovaciones(Membresia membresia, Long socioId) {
        var siguientes = membresiaRepository.findBySocioId(socioId).stream()
                .filter(m -> m.getEstado() == EstadoMembresia.PENDIENTE && m.getCodigoPreinscripcion() == null
                        && m.getFechaInicio() != null && m.getFechaFin() != null
                        && m.getFechaInicio().isAfter(membresia.getFechaInicio()))
                .sorted(Comparator.comparing(Membresia::getFechaInicio))
                .toList();
        LocalDate finAnterior = membresia.getFechaFin();
        for (Membresia siguiente : siguientes) {
            LocalDate nuevaFecha = finAnterior.plusDays(1);
            if (siguiente.getFechaInicio().isBefore(nuevaFecha)) {
                long duracion = java.time.temporal.ChronoUnit.DAYS.between(siguiente.getFechaInicio(), siguiente.getFechaFin());
                siguiente.setFechaInicio(nuevaFecha);
                siguiente.setFechaFin(nuevaFecha.plusDays(duracion));
            }
            finAnterior = siguiente.getFechaFin();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean membresiaCongelada(Long membresiaId, LocalDate fecha) {
        return hayCruce(membresiaId, fecha, fecha);
    }

    private boolean hayCruce(Long membresiaId, LocalDate inicio, LocalDate fin) {
        return congelamientoRepository.findByMembresiaIdAndEstadoAndFechaInicioSolicitadaLessThanEqual(
                membresiaId, EstadoSolicitudCongelamiento.APROBADA, fin)
                .stream().anyMatch(solicitud -> !ultimaFecha(solicitud).isBefore(inicio));
    }

    private static LocalDate ultimaFecha(SolicitudCongelamiento solicitud) {
        return solicitud.getFechaInicioSolicitada().plusDays(solicitud.getDiasSolicitados() - 1L);
    }
}
