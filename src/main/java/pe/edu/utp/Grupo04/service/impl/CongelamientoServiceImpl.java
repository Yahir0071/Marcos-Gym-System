package pe.edu.utp.Grupo04.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo04.dto.SolicitudCongelamientoDTO;
import pe.edu.utp.Grupo04.model.Membresia;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.model.SolicitudCongelamiento;
import pe.edu.utp.Grupo04.model.enums.EstadoSolicitudCongelamiento;
import pe.edu.utp.Grupo04.repository.SolicitudCongelamientoRepository;
import pe.edu.utp.Grupo04.service.CongelamientoService;
import pe.edu.utp.Grupo04.service.MembresiaService;
import pe.edu.utp.Grupo04.service.SocioService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class CongelamientoServiceImpl implements CongelamientoService {

    private final SolicitudCongelamientoRepository congelamientoRepository;
    private final SocioService socioService;
    private final MembresiaService membresiaService;

    public CongelamientoServiceImpl(SolicitudCongelamientoRepository congelamientoRepository,
                                    SocioService socioService,
                                    MembresiaService membresiaService) {
        this.congelamientoRepository = congelamientoRepository;
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

        Membresia membresiaActiva = membresiaService.obtenerMembresiaActivaPorUsername(username)
                .orElseThrow(() -> new IllegalStateException("Solo los socios con membresía activa pueden solicitar congelamiento."));

        if (dto.getFechaInicioSolicitada() == null || dto.getFechaInicioSolicitada().isBefore(LocalDate.now())) {
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
        solicitud.setFechaSolicitud(LocalDateTime.now());

        return congelamientoRepository.save(solicitud);
    }
}
