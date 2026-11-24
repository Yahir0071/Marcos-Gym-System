package pe.edu.utp.Grupo04.service;

import org.springframework.data.domain.Slice;
import pe.edu.utp.Grupo04.dto.SolicitudCongelamientoDTO;
import pe.edu.utp.Grupo04.model.SolicitudCongelamiento;
import pe.edu.utp.Grupo04.model.enums.EstadoSolicitudCongelamiento;

import java.time.LocalDate;
import java.util.List;

public interface CongelamientoService {
    List<SolicitudCongelamiento> listarPorUsername(String username);
    SolicitudCongelamiento registrarSolicitud(String username, SolicitudCongelamientoDTO dto);
    Slice<SolicitudCongelamiento> listarParaRevision(EstadoSolicitudCongelamiento estado, int pagina);
    void resolver(Long id, EstadoSolicitudCongelamiento decision, String operador);
    boolean membresiaCongelada(Long membresiaId, LocalDate fecha);
}
