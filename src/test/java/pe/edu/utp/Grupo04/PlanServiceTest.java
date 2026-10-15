package pe.edu.utp.Grupo04;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.utp.Grupo04.model.Plan;
import pe.edu.utp.Grupo04.repository.PlanRepository;
import pe.edu.utp.Grupo04.service.impl.PlanServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PlanServiceTest {

    @Mock
    private PlanRepository planRepository;

    @InjectMocks
    private PlanServiceImpl planService;

    private Plan planActivo;
    private Plan planInactivo;

    @BeforeEach
    void setUp() {
        planActivo = new Plan();
        planActivo.setId(1L);
        planActivo.setNombre("Plan Trimestral Pro");
        planActivo.setDescripcion("Acceso total a sala de pesas y cardio");
        planActivo.setDuracionDias(90);
        planActivo.setPrecio(299.00);
        planActivo.setActivo(true);

        planInactivo = new Plan();
        planInactivo.setId(2L);
        planInactivo.setNombre("Plan Verano Antiguo");
        planInactivo.setDescripcion("Promoción anterior");
        planInactivo.setDuracionDias(30);
        planInactivo.setPrecio(69.00);
        planInactivo.setActivo(false);
    }

    @Test
    @DisplayName("RF10 - Debe listar únicamente los planes con estado activo")
    void testListarActivos() {
        when(planRepository.findByActivoTrue()).thenReturn(List.of(planActivo));

        List<Plan> activos = planService.listarActivos();

        assertNotNull(activos, "La lista de planes activos no debe ser nula");
        assertEquals(1, activos.size(), "Debe retornar exactamente 1 plan activo");
        assertTrue(activos.get(0).getActivo(), "El plan retornado debe estar activo");
        verify(planRepository, times(1)).findByActivoTrue();
    }

    @Test
    @DisplayName("RF10 - Debe guardar o actualizar un plan exitosamente")
    void testGuardarPlan() {
        when(planRepository.save(any(Plan.class))).thenReturn(planActivo);

        Plan guardado = planService.guardar(planActivo);

        assertNotNull(guardado, "El plan guardado no debe ser nulo");
        assertEquals("Plan Trimestral Pro", guardado.getNombre());
        assertEquals(299.00, guardado.getPrecio());
        verify(planRepository, times(1)).save(planActivo);
    }

    @Test
    @DisplayName("RF10 - Debe cambiar el estado de activo a inactivo correctamente")
    void testCambiarEstado() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(planActivo));
        when(planRepository.save(any(Plan.class))).thenAnswer(i -> i.getArgument(0));

        planService.cambiarEstado(1L, false);

        assertFalse(planActivo.getActivo(), "El estado del plan debe haber cambiado a false (inactivo)");
        verify(planRepository, times(1)).findById(1L);
        verify(planRepository, times(1)).save(planActivo);
    }

    @Test
    @DisplayName("RF10 - Debe buscar un plan por ID existente")
    void testBuscarPorId() {
        when(planRepository.findById(1L)).thenReturn(Optional.of(planActivo));

        Optional<Plan> resultado = planService.buscarPorId(1L);

        assertTrue(resultado.isPresent(), "El plan debe ser encontrado");
        assertEquals("Plan Trimestral Pro", resultado.get().getNombre());
        verify(planRepository, times(1)).findById(1L);
    }
}
