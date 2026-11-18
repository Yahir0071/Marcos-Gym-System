package pe.edu.utp.Grupo04;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.utp.Grupo04.exception.AccesoDenegadoException;
import pe.edu.utp.Grupo04.model.Producto;
import pe.edu.utp.Grupo04.model.Reserva;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.model.Usuario;
import pe.edu.utp.Grupo04.model.enums.EstadoReserva;
import pe.edu.utp.Grupo04.repository.ProductoRepository;
import pe.edu.utp.Grupo04.repository.ReservaRepository;
import pe.edu.utp.Grupo04.repository.SocioRepository;
import pe.edu.utp.Grupo04.service.ProductoService;
import pe.edu.utp.Grupo04.service.impl.ReservaServiceImpl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private SocioRepository socioRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private ProductoService productoService;

    @InjectMocks
    private ReservaServiceImpl reservaService;

    private Socio socio;
    private Usuario usuario;
    private Producto producto;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setUsername("carlos@gmail.com");

        socio = new Socio();
        socio.setId(1L);
        socio.setNombres("Carlos");
        socio.setUsuario(usuario);

        producto = new Producto();
        producto.setId(10L);
        producto.setNombre("Whey Protein");
        producto.setStock(5);
        producto.setActivo(true);
    }

    @Test
    @DisplayName("Debe crear una reserva exitosa y descontar stock preventivamente")
    void testCrearReservaExitosa() {
        when(socioRepository.findByUsuarioUsername("carlos@gmail.com")).thenReturn(Optional.of(socio));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));
        when(productoService.hayStockSuficiente(10L, 2)).thenReturn(true);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reserva reserva = reservaService.crearReserva("carlos@gmail.com", 10L, 2);

        assertNotNull(reserva);
        assertEquals(2, reserva.getCantidad());
        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstado());
        verify(productoService, times(1)).descontarStock(10L, 2);
        verify(reservaRepository, times(1)).save(any(Reserva.class));
    }

    @Test
    @DisplayName("Debe rechazar la reserva si no hay stock suficiente (RNF07)")
    void testCrearReservaSinStock() {
        when(socioRepository.findByUsuarioUsername("carlos@gmail.com")).thenReturn(Optional.of(socio));
        when(productoRepository.findById(10L)).thenReturn(Optional.of(producto));
        when(productoService.hayStockSuficiente(10L, 10)).thenReturn(false);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            reservaService.crearReserva("carlos@gmail.com", 10L, 10);
        });

        assertTrue(ex.getMessage().toLowerCase().contains("stock"));
        verify(productoService, never()).descontarStock(anyLong(), anyInt());
    }

    @Test
    @DisplayName("Debe restaurar el inventario si el administrador cancela una reserva pendiente")
    void testCancelarReservaRestauraStock() {
        Reserva reserva = new Reserva();
        reserva.setId(100L);
        reserva.setProducto(producto);
        reserva.setCantidad(3);
        reserva.setEstado(EstadoReserva.PENDIENTE);

        when(reservaRepository.findById(100L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Reserva cancelada = reservaService.actualizarEstado(100L, EstadoReserva.CANCELADA);

        assertEquals(EstadoReserva.CANCELADA, cancelada.getEstado());
        verify(productoService, times(1)).restaurarStock(10L, 3);
    }

    @Test
    @DisplayName("C03 - El cliente dueño de la reserva sí puede cancelarla y se restaura el inventario")
    void testCancelarReservaPorCliente_Exitoso() {
        Reserva reserva = new Reserva();
        reserva.setId(100L);
        reserva.setSocio(socio);
        reserva.setProducto(producto);
        reserva.setCantidad(2);
        reserva.setEstado(EstadoReserva.PENDIENTE);

        when(reservaRepository.findById(100L)).thenReturn(Optional.of(reserva));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(i -> i.getArgument(0));

        Reserva cancelada = reservaService.cancelarReservaPorCliente(100L, "carlos@gmail.com");

        assertEquals(EstadoReserva.CANCELADA, cancelada.getEstado());
        verify(productoService, times(1)).restaurarStock(10L, 2);
    }

    @Test
    @DisplayName("C03 - Debe rechazar con AccesoDenegadoException si un cliente intenta cancelar la reserva de otro")
    void testCancelarReservaPorCliente_RechazaReservaAjena() {
        Reserva reserva = new Reserva();
        reserva.setId(100L);
        reserva.setSocio(socio); // Pertenece a carlos@gmail.com
        reserva.setProducto(producto);
        reserva.setCantidad(2);
        reserva.setEstado(EstadoReserva.PENDIENTE);

        when(reservaRepository.findById(100L)).thenReturn(Optional.of(reserva));

        // Intento de cancelación por otro usuario (juan@gmail.com)
        assertThrows(AccesoDenegadoException.class, () -> {
            reservaService.cancelarReservaPorCliente(100L, "juan@gmail.com");
        });

        verify(productoService, never()).restaurarStock(anyLong(), anyInt());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("M12 - No permite doble cancelación ni duplicar devolución de stock")
    void testCancelarReservaPorCliente_NoPermiteDobleCancelacion() {
        Reserva reserva = new Reserva();
        reserva.setId(100L);
        reserva.setSocio(socio);
        reserva.setProducto(producto);
        reserva.setCantidad(2);
        reserva.setEstado(EstadoReserva.CANCELADA); // Ya cancelada previamente

        when(reservaRepository.findById(100L)).thenReturn(Optional.of(reserva));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            reservaService.cancelarReservaPorCliente(100L, "carlos@gmail.com");
        });

        assertTrue(ex.getMessage().contains("previamente"));
        verify(productoService, never()).restaurarStock(anyLong(), anyInt());
    }
}
