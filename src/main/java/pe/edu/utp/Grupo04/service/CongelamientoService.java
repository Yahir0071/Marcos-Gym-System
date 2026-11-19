package pe.edu.utp.Grupo04.service;

import pe.edu.utp.Grupo04.dto.SolicitudCongelamientoDTO;
import pe.edu.utp.Grupo04.model.SolicitudCongelamiento;

import java.util.List;

public interface CongelamientoService {
    List<SolicitudCongelamiento> listarPorUsername(String username);
    SolicitudCongelamiento registrarSolicitud(String username, SolicitudCongelamientoDTO dto);
}
