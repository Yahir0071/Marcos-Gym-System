package pe.edu.utp.Grupo04;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.utp.Grupo04.model.Membresia;
import pe.edu.utp.Grupo04.model.Pago;
import pe.edu.utp.Grupo04.model.Plan;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.model.enums.EstadoMembresia;
import pe.edu.utp.Grupo04.repository.MembresiaRepository;
import pe.edu.utp.Grupo04.repository.PagoRepository;
import pe.edu.utp.Grupo04.service.impl.PagoServiceImpl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PagoServiceTest {

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private MembresiaRepository membresiaRepository;

    @InjectMocks
    private PagoServiceImpl pagoService;

    private Membresia membresiaPendiente;

    @BeforeEach
    void setUp() {
        Socio socio = new Socio();
        socio.setId(1L);
        socio.setNombres("Carlos");
        socio.setApellidos("Mendoza");

        Plan plan = new Plan();
        plan.setId(1L);
        plan.setNombre("Plan Plus");
        plan.setPrecio(119.00);

        membresiaPendiente = new Membresia();
        membresiaPendiente.setId(100L);
        membresiaPendiente.setSocio(socio);
        membresiaPendiente.setPlan(plan);
        membresiaPendiente.setEstado(EstadoMembresia.PENDIENTE);
    }

    @Test
    @DisplayName("RF12 - Debe registrar el pago presencial y cambiar el estado de la membresía a ACTIVA")
    void testRegistrarPagoPresencial_Exitoso() {
        when(membresiaRepository.findById(100L)).thenReturn(Optional.of(membresiaPendiente));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(i -> {
            Pago p = i.getArgument(0);
            p.setId(1L);
            return p;
        });

        Pago resultado = pagoService.registrarPagoPresencial(100L, 119.00, "Efectivo");

        assertNotNull(resultado, "El pago guardado no debe ser nulo");
        assertEquals(119.00, resultado.getMonto());
        assertEquals("Efectivo", resultado.getMetodoPago());
        assertEquals(EstadoMembresia.ACTIVA, membresiaPendiente.getEstado(), "La membresía debe activarse inmediatamente");
        verify(membresiaRepository, times(1)).save(membresiaPendiente);
        verify(pagoRepository, times(1)).save(any(Pago.class));
    }

    @Test
    @DisplayName("RF12 - Debe lanzar IllegalArgumentException si la membresía a cobrar no existe")
    void testRegistrarPagoPresencial_MembresiaInexistente() {
        when(membresiaRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            pagoService.registrarPagoPresencial(999L, 100.00, "Tarjeta");
        });

        assertTrue(ex.getMessage().contains("Membresía no encontrada"));
        verify(pagoRepository, never()).save(any(Pago.class));
    }
}
