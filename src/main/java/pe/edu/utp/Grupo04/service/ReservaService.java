package pe.edu.utp.Grupo04.service;

import pe.edu.utp.Grupo04.model.Reserva;
import pe.edu.utp.Grupo04.model.enums.EstadoReserva;

import java.util.List;
import java.util.Optional;

public interface ReservaService {
    List<Reserva> listarTodas();
    List<Reserva> listarPorSocioUsername(String username);
    List<Reserva> listarPorSocioId(Long socioId);
    Optional<Reserva> buscarPorId(Long id);
    Reserva crearReserva(String username, Long productoId, int cantidad);
    Reserva actualizarEstado(Long reservaId, EstadoReserva nuevoEstado);
    long contarPendientes();
}
