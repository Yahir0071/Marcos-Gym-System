package pe.edu.utp.Grupo04;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.utp.Grupo04.model.Membresia;
import pe.edu.utp.Grupo04.model.Plan;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.model.enums.EstadoMembresia;
import pe.edu.utp.Grupo04.repository.MembresiaRepository;
import pe.edu.utp.Grupo04.repository.PlanRepository;
import pe.edu.utp.Grupo04.repository.SocioRepository;
import pe.edu.utp.Grupo04.service.TarifaInscripcion;
import pe.edu.utp.Grupo04.service.impl.MembresiaServiceImpl;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MembresiaServiceTest {

    @Mock
    private MembresiaRepository membresiaRepository;

    @Mock
    private SocioRepository socioRepository;

    @Mock
    private PlanRepository planRepository;

    @InjectMocks
    private MembresiaServiceImpl membresiaService;

    private Socio socio;
    private Plan plan;

    @BeforeEach
    void setUp() {
        socio = new Socio();
        socio.setId(1L);
        socio.setDni("72563123");
        socio.setNombres("Carlos");
        socio.setApellidos("Mendoza");

        plan = new Plan();
        plan.setId(1L);
        plan.setNombre("Plan Plus");
        plan.setDuracionDias(30);
        plan.setPrecio(89.90);
    }

    @Test
    @DisplayName("Debe calcular correctamente la fecha de fin y estado activo al asignar plan mensual")
    void testAsignarPlanASocio_Mensual() {
        when(socioRepository.findById(1L)).thenReturn(Optional.of(socio));
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(membresiaRepository.findBySocioId(1L)).thenReturn(Collections.emptyList());
        when(membresiaRepository.save(any(Membresia.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDate hoy = LocalDate.now();
        Membresia membresia = membresiaService.asignarPlanASocio(1L, 1L, hoy);

        assertNotNull(membresia);
        assertEquals(hoy, membresia.getFechaInicio());
        assertEquals(hoy.plusDays(30), membresia.getFechaFin());
        assertEquals(EstadoMembresia.ACTIVA, membresia.getEstado());
        verify(membresiaRepository, times(1)).save(any(Membresia.class));
    }

    @Test
    @DisplayName("C09 - Debe otorgar exactamente 12 meses (1 año) de vigencia en contratación ANUAL")
    void testAsignarPlanASocio_AnualDura12Meses() {
        when(socioRepository.findById(1L)).thenReturn(Optional.of(socio));
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(membresiaRepository.findBySocioId(1L)).thenReturn(Collections.emptyList());
        when(membresiaRepository.save(any(Membresia.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDate hoy = LocalDate.now();
        Membresia membresia = membresiaService.asignarPlanASocio(1L, 1L, hoy, "ANUAL", 12);

        assertNotNull(membresia);
        assertEquals(hoy, membresia.getFechaInicio());
        assertEquals(hoy.plusYears(1), membresia.getFechaFin());
        assertEquals(EstadoMembresia.ACTIVA, membresia.getEstado());
    }

    @Test
    @DisplayName("C09 - La renovación de membresía debe empezar al día siguiente del vencimiento actual sin perder días")
    void testAsignarPlanASocio_RenovacionMembresiaActiva() {
        LocalDate hoy = LocalDate.now();
        LocalDate finActual = hoy.plusDays(15); // Le quedan 15 días

        Membresia actual = new Membresia();
        actual.setId(10L);
        actual.setSocio(socio);
        actual.setPlan(plan);
        actual.setFechaInicio(hoy.minusDays(15));
        actual.setFechaFin(finActual);
        actual.setEstado(EstadoMembresia.ACTIVA);

        when(socioRepository.findById(1L)).thenReturn(Optional.of(socio));
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(membresiaRepository.findBySocioId(1L)).thenReturn(List.of(actual));
        when(membresiaRepository.save(any(Membresia.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Renueva por un año
        Membresia renovacion = membresiaService.asignarPlanASocio(1L, 1L, hoy, "ANUAL", 12);

        assertNotNull(renovacion);
        assertEquals(finActual.plusDays(1), renovacion.getFechaInicio());
        assertEquals(finActual.plusDays(1).plusYears(1), renovacion.getFechaFin());
    }

    @Test
    @DisplayName("Debe calcular los días restantes correctamente para una membresía vigente")
    void testCalcularDiasRestantes() {
        Membresia membresia = new Membresia();
        membresia.setFechaFin(LocalDate.now().plusDays(15));

        long dias = membresiaService.calcularDiasRestantes(membresia);

        assertEquals(15, dias);
    }

    @Test
    void activaProgramadaEnSuFechaYConservaPreinscripcionPendiente() {
        LocalDate hoy = LocalDate.now(TarifaInscripcion.ZONA_LIMA);
        Membresia programada = new Membresia();
        programada.setEstado(EstadoMembresia.PENDIENTE);
        programada.setFechaInicio(hoy);
        programada.setFechaFin(hoy.plusDays(30));
        Membresia preinscripcion = new Membresia();
        preinscripcion.setEstado(EstadoMembresia.PENDIENTE);
        preinscripcion.setFechaInicio(hoy);
        preinscripcion.setFechaFin(hoy.plusDays(30));
        preinscripcion.setCodigoPreinscripcion("INS-QA");
        when(socioRepository.findByUsuarioUsername("socio@example.test")).thenReturn(Optional.of(socio));
        when(membresiaRepository.findBySocioId(socio.getId())).thenReturn(List.of(preinscripcion, programada));

        assertEquals(programada, membresiaService.obtenerMembresiaActivaPorUsername("socio@example.test").orElseThrow());
        assertEquals(EstadoMembresia.ACTIVA, programada.getEstado());
        assertEquals(EstadoMembresia.PENDIENTE, preinscripcion.getEstado());
        verify(membresiaRepository, never()).save(preinscripcion);
    }

    @Test
    void programaSiguienteRenovacionDespuesDeLaUltimaVigencia() {
        LocalDate hoy = LocalDate.now(TarifaInscripcion.ZONA_LIMA);
        Membresia primera = new Membresia();
        primera.setFechaFin(hoy.plusDays(15));
        primera.setEstado(EstadoMembresia.ACTIVA);
        Membresia segunda = new Membresia();
        segunda.setFechaFin(hoy.plusDays(45));
        segunda.setEstado(EstadoMembresia.PENDIENTE);
        when(socioRepository.findById(1L)).thenReturn(Optional.of(socio));
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(membresiaRepository.findBySocioId(1L)).thenReturn(List.of(primera, segunda));
        when(membresiaRepository.save(any(Membresia.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Membresia renovacion = membresiaService.asignarPlanASocio(1L, 1L, hoy);

        assertEquals(hoy.plusDays(46), renovacion.getFechaInicio());
        assertEquals(EstadoMembresia.PENDIENTE, renovacion.getEstado());
    }
}
