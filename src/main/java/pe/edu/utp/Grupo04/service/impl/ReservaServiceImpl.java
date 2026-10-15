package pe.edu.utp.Grupo04.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
        return reservaRepository.findById(id);
    }

    @Override
    public Reserva crearReserva(String username, Long productoId, int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero.");
        }

        Socio socio = socioRepository.findByUsuarioUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Socio no encontrado para el usuario: " + username));

        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado con ID: " + productoId));

        if (!productoService.hayStockSuficiente(productoId, cantidad)) {
            throw new IllegalStateException("No hay suficiente stock disponible para: " + producto.getNombre());
        }

        // Descontar inventario preventivamente
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
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada con ID: " + reservaId));

        EstadoReserva estadoAnterior = reserva.getEstado();

        // Si se cancela una reserva pendiente o confirmada, restaurar el stock
        if (nuevoEstado == EstadoReserva.CANCELADA &&
                (estadoAnterior == EstadoReserva.PENDIENTE || estadoAnterior == EstadoReserva.CONFIRMADA)) {
            productoService.restaurarStock(reserva.getProducto().getId(), reserva.getCantidad());
        }

        reserva.setEstado(nuevoEstado);
        return reservaRepository.save(reserva);
    }

    @Override
    @Transactional(readOnly = true)
    public long contarPendientes() {
        return reservaRepository.countByEstado(EstadoReserva.PENDIENTE);
    }
}
