package pe.edu.utp.Grupo04.service;

import pe.edu.utp.Grupo04.model.Equipo;
import pe.edu.utp.Grupo04.model.enums.EstadoEquipo;

import java.util.List;
import java.util.Optional;

public interface EquipoService {
    List<Equipo> listarTodos();
    List<Equipo> listarPorEstado(EstadoEquipo estado);
    Optional<Equipo> buscarPorId(Long id);
    Equipo guardar(Equipo equipo);
    Equipo cambiarEstado(Long id, EstadoEquipo nuevoEstado);
    long contarEquipos();
    long contarPorEstado(EstadoEquipo estado);
}
