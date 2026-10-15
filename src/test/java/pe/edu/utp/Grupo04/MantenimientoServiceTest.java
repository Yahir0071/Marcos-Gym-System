package pe.edu.utp.Grupo04;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.utp.Grupo04.model.Equipo;
import pe.edu.utp.Grupo04.model.Mantenimiento;
import pe.edu.utp.Grupo04.model.enums.EstadoEquipo;
import pe.edu.utp.Grupo04.repository.EquipoRepository;
import pe.edu.utp.Grupo04.repository.MantenimientoRepository;
import pe.edu.utp.Grupo04.service.impl.MantenimientoServiceImpl;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MantenimientoServiceTest {

    @Mock
    private MantenimientoRepository mantenimientoRepository;

    @Mock
    private EquipoRepository equipoRepository;

    @InjectMocks
    private MantenimientoServiceImpl mantenimientoService;

    private Equipo equipoOperativo;

    @BeforeEach
    void setUp() {
        equipoOperativo = new Equipo();
        equipoOperativo.setId(5L);
        equipoOperativo.setCodigo("EQP-005");
        equipoOperativo.setNombre("Prensa Inclinada");
        equipoOperativo.setEstado(EstadoEquipo.OPERATIVO);
    }

    @Test
    @DisplayName("RF17 - Mantenimiento Correctivo debe cambiar el estado del equipo a EN_MANTENIMIENTO")
    void testMantenimientoCorrectivo_CambiaEstadoAEnMantenimiento() {
        when(equipoRepository.findById(5L)).thenReturn(Optional.of(equipoOperativo));
        when(mantenimientoRepository.save(any(Mantenimiento.class))).thenAnswer(i -> i.getArgument(0));

        Mantenimiento m = new Mantenimiento();
        m.setTipo("Correctivo");
        m.setDescripcion("Cambio de rodamientos dañados");
        m.setFechaMantenimiento(LocalDate.now());

        Mantenimiento registrado = mantenimientoService.registrarMantenimiento(5L, m);

        assertNotNull(registrado);
        assertEquals(EstadoEquipo.EN_MANTENIMIENTO, equipoOperativo.getEstado(),
                "Al ser mantenimiento Correctivo, la máquina debe pasar a EN_MANTENIMIENTO");
        verify(equipoRepository, times(1)).save(equipoOperativo);
        verify(mantenimientoRepository, times(1)).save(m);
    }

    @Test
    @DisplayName("RF17 - Mantenimiento Preventivo debe registrarse sin inhabilitar el equipo si está operativo")
    void testMantenimientoPreventivo_ConservaEstadoOperativo() {
        when(equipoRepository.findById(5L)).thenReturn(Optional.of(equipoOperativo));
        when(mantenimientoRepository.save(any(Mantenimiento.class))).thenAnswer(i -> i.getArgument(0));

        Mantenimiento m = new Mantenimiento();
        m.setTipo("Preventivo");
        m.setDescripcion("Lubricación periódica");
        m.setFechaMantenimiento(LocalDate.now());

        Mantenimiento registrado = mantenimientoService.registrarMantenimiento(5L, m);

        assertNotNull(registrado);
        assertEquals(EstadoEquipo.OPERATIVO, equipoOperativo.getEstado(),
                "Al ser mantenimiento Preventivo, la máquina se mantiene OPERATIVO");
        verify(equipoRepository, never()).save(equipoOperativo);
        verify(mantenimientoRepository, times(1)).save(m);
    }
}
