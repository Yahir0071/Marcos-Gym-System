package pe.edu.utp.Grupo04.service;

import pe.edu.utp.Grupo04.model.RegistroAfluencia;
import pe.edu.utp.Grupo04.model.enums.FranjaHoraria;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface AfluenciaService {
    int CAPACIDAD_MAXIMA = 80;

    List<RegistroAfluencia> listarTodos();
    List<RegistroAfluencia> listarPorFecha(LocalDate fecha);
    RegistroAfluencia registrarOActualizar(LocalDate fecha, FranjaHoraria franjaHoraria, Integer concurrencia);
    int incrementarPorAcceso(LocalDate fecha, FranjaHoraria franjaHoraria, int capacidadMaxima);
    Map<FranjaHoraria, Integer> obtenerAfluenciaVigente();
    LocalDateTime obtenerUltimaFechaActualizacion();
}
