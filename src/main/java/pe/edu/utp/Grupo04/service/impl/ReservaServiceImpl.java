package pe.edu.utp.Grupo04.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo04.exception.AccesoDenegadoException;
import pe.edu.utp.Grupo04.exception.RecursoNoEncontradoException;
import pe.edu.utp.Grupo04.model.Producto;
import pe.edu.utp.Grupo04.model.Reserva;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.model.enums.EstadoReserva;
import pe.edu.utp.Grupo04.repository.ProductoRepository;
import pe.edu.utp.Grupo04.repository.ReservaRepository;
import pe.edu.utp.Grupo04.repository.SocioRepository;
import pe.edu.utp.Grupo04.service.ProductoService;
import pe.edu.utp.Grupo04.service.ReservaService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReservaServiceImpl implements ReservaService {

    private final ReservaRepository reservaRepository;
    private final SocioRepository socioRepository;
    private final ProductoRepository productoRepository;
    private final ProductoService productoService;

    public ReservaServiceImpl(ReservaRepository reservaRepository,
                              SocioRepository socioRepository,
                              ProductoRepository productoRepository,
                              ProductoService productoService) {
        this.reservaRepository = reservaRepository;
        this.socioRepository = socioRepository;
        this.productoRepository = productoRepository;
        this.productoService = productoService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reserva> listarTodas() {
        return reservaRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reserva> listarPorSocioUsername(String username) {
        return reservaRepository.findBySocioUsuarioUsernameOrderByFechaReservaDesc(username);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Reserva> listarPorSocioId(Long socioId) {
        return reservaRepository.findBySocioIdOrderByFechaReservaDesc(socioId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Reserva> buscarPorId(Long id) {
        if (id == null) return Optional.empty();
        return reservaRepository.findById(id);
    }

    @Override
    public Reserva crearReserva(String username, Long productoId, int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero.");
        }

        Socio socio = socioRepository.findByUsuarioUsername(username)
                .orElseThrow(() -> new RecursoNoEncontradoException("Socio no encontrado para el usuario: " + username));

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado con ID: " + productoId));

        if (!productoService.hayStockSuficiente(productoId, cantidad)) {
            throw new IllegalStateException("No hay suficiente stock disponible para: " + producto.getNombre());
        }

        // Descontar inventario preventivamente de manera atómica
        productoService.descontarStock(productoId, cantidad);

        Reserva reserva = new Reserva();
        reserva.setSocio(socio);
        reserva.setProducto(producto);
        reserva.setCantidad(cantidad);
        reserva.setFechaReserva(LocalDateTime.now());
        reserva.setEstado(EstadoReserva.PENDIENTE);

        return reservaRepository.save(reserva);
    }

    @Override
    public Reserva actualizarEstado(Long reservaId, EstadoReserva nuevoEstado) {
        Reserva reserva = reservaRepository.findLockedById(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada con ID: " + reservaId));

        EstadoReserva estadoAnterior = reserva.getEstado();
        if (estadoAnterior == nuevoEstado) {
            return reserva;
        }
        if (estadoAnterior == EstadoReserva.CANCELADA || estadoAnterior == EstadoReserva.ENTREGADA) {
            throw new IllegalStateException("Las reservas canceladas o entregadas no pueden cambiar de estado.");
        }

        // Si se cancela una reserva pendiente o confirmada, restaurar el stock una sola vez
        if (nuevoEstado == EstadoReserva.CANCELADA &&
                (estadoAnterior == EstadoReserva.PENDIENTE || estadoAnterior == EstadoReserva.CONFIRMADA)) {
            productoService.restaurarStock(reserva.getProducto().getId(), reserva.getCantidad());
        }

        reserva.setEstado(nuevoEstado);
        return reservaRepository.save(reserva);
    }

    @Override
    public Reserva cancelarReservaPorCliente(Long reservaId, String username) {
        Reserva reserva = reservaRepository.findLockedById(reservaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reserva no encontrada con ID: " + reservaId));

        // C03: Verificar propiedad estricta
        if (reserva.getSocio() == null || reserva.getSocio().getUsuario() == null ||
                !reserva.getSocio().getUsuario().getUsername().equalsIgnoreCase(username)) {
            throw new AccesoDenegadoException("Acceso denegado: no tienes permisos para cancelar reservas de otro socio.");
        }

        // M12: Validar transiciones de estado e idempotencia
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new IllegalStateException("La reserva ya ha sido cancelada previamente.");
        }

        if (reserva.getEstado() == EstadoReserva.ENTREGADA) {
            throw new IllegalStateException("No se puede cancelar una reserva que ya ha sido entregada en recepción.");
        }

        // Restaurar inventario una única vez
        if (reserva.getEstado() == EstadoReserva.PENDIENTE || reserva.getEstado() == EstadoReserva.CONFIRMADA) {
            productoService.restaurarStock(reserva.getProducto().getId(), reserva.getCantidad());
        }

        reserva.setEstado(EstadoReserva.CANCELADA);
        return reservaRepository.save(reserva);
    }

    @Override
    @Transactional(readOnly = true)
    public long contarPendientes() {
        return reservaRepository.countByEstado(EstadoReserva.PENDIENTE);
    }
}
