package pe.edu.utp.Grupo04.service;

import pe.edu.utp.Grupo04.model.Mantenimiento;

import java.util.List;

public interface MantenimientoService {
    List<Mantenimiento> listarTodos();
    List<Mantenimiento> listarPorEquipoId(Long equipoId);
    Mantenimiento registrarMantenimiento(Long equipoId, Mantenimiento mantenimiento);
}
