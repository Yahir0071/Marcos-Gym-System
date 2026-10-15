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
import pe.edu.utp.Grupo04.service.impl.MembresiaServiceImpl;

import java.time.LocalDate;
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
    @DisplayName("Debe calcular correctamente la fecha de fin y estado activo al asignar plan")
    void testAsignarPlanExitoso() {
        LocalDate hoy = LocalDate.now();

        when(socioRepository.findById(1L)).thenReturn(Optional.of(socio));
        when(planRepository.findById(1L)).thenReturn(Optional.of(plan));
        when(membresiaRepository.save(any(Membresia.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Membresia membresia = membresiaService.asignarPlanASocio(1L, 1L, hoy);

        assertNotNull(membresia);
        assertEquals(hoy, membresia.getFechaInicio());
        assertEquals(hoy.plusDays(30), membresia.getFechaFin());
        assertEquals(EstadoMembresia.ACTIVA, membresia.getEstado());
        verify(membresiaRepository, times(1)).save(any(Membresia.class));
    }

    @Test
    @DisplayName("Debe detectar alerta de membresía próxima a vencer cuando quedan 5 días")
    void testEstaPorVencer() {
        Membresia membresia = new Membresia();
        membresia.setEstado(EstadoMembresia.ACTIVA);
        membresia.setFechaFin(LocalDate.now().plusDays(5));

        boolean alerta = membresiaService.estaPorVencer(membresia, 7);

        assertTrue(alerta, "Debe alertar porque 5 días es menor o igual al umbral de 7 días");
        assertEquals(5, membresiaService.calcularDiasRestantes(membresia));
    }
}
