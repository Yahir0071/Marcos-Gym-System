package pe.edu.utp.Grupo04.service;

import pe.edu.utp.Grupo04.model.RegistroAfluencia;
import pe.edu.utp.Grupo04.model.enums.FranjaHoraria;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AfluenciaService {
    List<RegistroAfluencia> listarTodos();
    List<RegistroAfluencia> listarPorFecha(LocalDate fecha);
    RegistroAfluencia registrarOActualizar(LocalDate fecha, FranjaHoraria franjaHoraria, Integer concurrencia);
    Map<FranjaHoraria, Integer> obtenerAfluenciaVigente();
    LocalDateTime obtenerUltimaFechaActualizacion();
}
