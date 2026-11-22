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
import pe.edu.utp.Grupo04.exception.ValidacionNegocioException;
import pe.edu.utp.Grupo04.repository.MembresiaRepository;
import pe.edu.utp.Grupo04.repository.PagoRepository;
import pe.edu.utp.Grupo04.service.impl.PagoServiceImpl;
import pe.edu.utp.Grupo04.service.TarifaInscripcion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
        membresiaPendiente.setPeriodoReservado("MENSUAL");
        membresiaPendiente.setMontoReservado(new BigDecimal("89.00"));
        membresiaPendiente.setFechaInicio(LocalDate.now(TarifaInscripcion.ZONA_LIMA).minusDays(1));
        membresiaPendiente.setFechaFin(membresiaPendiente.getFechaInicio().plusDays(30));
        membresiaPendiente.setPreinscripcionVence(LocalDateTime.now(TarifaInscripcion.ZONA_LIMA).plusHours(24));
    }

    @Test
    @DisplayName("RF12 - Debe registrar el pago presencial y cambiar el estado de la membresía a ACTIVA")
    void testRegistrarPagoPresencial_Exitoso() {
        when(membresiaRepository.buscarParaActualizar(100L)).thenReturn(Optional.of(membresiaPendiente));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(i -> {
            Pago p = i.getArgument(0);
            p.setId(1L);
            return p;
        });

        Pago resultado = pagoService.registrarPagoPresencial(100L, new BigDecimal("89.00"), "Efectivo", "CAJA-ABC");

        assertNotNull(resultado, "El pago guardado no debe ser nulo");
        assertEquals(89.00, resultado.getMonto());
        assertEquals("Efectivo", resultado.getMetodoPago());
        assertEquals("CAJA-ABC", resultado.getReferencia());
        assertEquals(LocalDate.now(TarifaInscripcion.ZONA_LIMA), membresiaPendiente.getFechaInicio());
        assertEquals(EstadoMembresia.ACTIVA, membresiaPendiente.getEstado(), "La membresía debe activarse inmediatamente");
        verify(membresiaRepository, times(1)).save(membresiaPendiente);
        verify(pagoRepository, times(1)).save(any(Pago.class));
    }

    @Test
    @DisplayName("RF12 - Debe rechazar un pago para una membresía inexistente")
    void testRegistrarPagoPresencial_MembresiaInexistente() {
        when(membresiaRepository.buscarParaActualizar(999L)).thenReturn(Optional.empty());

        assertThrows(ValidacionNegocioException.class, () ->
                pagoService.registrarPagoPresencial(999L, new BigDecimal("100.00"), "Tarjeta", "CAJA-ABC"));

        verify(pagoRepository, never()).save(any(Pago.class));
    }

    @Test
    void rechazaPagoParcialSinActivar() {
        when(membresiaRepository.buscarParaActualizar(100L)).thenReturn(Optional.of(membresiaPendiente));
        assertThrows(ValidacionNegocioException.class, () ->
                pagoService.registrarPagoPresencial(100L, new BigDecimal("10.00"), "Efectivo", "CAJA-ABC"));
        assertEquals(EstadoMembresia.PENDIENTE, membresiaPendiente.getEstado());
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void reutilizaPagoConReferenciaIdentica() {
        when(membresiaRepository.buscarParaActualizar(100L)).thenReturn(Optional.of(membresiaPendiente));
        Pago pago = new Pago();
        pago.setMembresia(membresiaPendiente);
        pago.setMonto(89.00);
        pago.setMetodoPago("Efectivo");
        when(pagoRepository.findByReferencia("CAJA-ABC")).thenReturn(Optional.of(pago));
        membresiaPendiente.setEstado(EstadoMembresia.ACTIVA);
        assertSame(pago, pagoService.registrarPagoPresencial(100L, new BigDecimal("89.00"), "Efectivo", "caja-abc"));
        verify(pagoRepository, never()).save(any());
    }
}
