package pe.edu.utp.Grupo04;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.utp.Grupo04.model.RegistroAfluencia;
import pe.edu.utp.Grupo04.model.enums.FranjaHoraria;
import pe.edu.utp.Grupo04.repository.RegistroAfluenciaRepository;
import pe.edu.utp.Grupo04.service.impl.AfluenciaServiceImpl;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AfluenciaServiceTest {

    @Mock
    private RegistroAfluenciaRepository afluenciaRepository;

    @InjectMocks
    private AfluenciaServiceImpl afluenciaService;

    private LocalDate hoy;

    @BeforeEach
    void setUp() {
        hoy = LocalDate.now();
    }

    @Test
    @DisplayName("RF16 - Debe crear un nuevo registro de afluencia si no existe para la fecha y franja")
    void testRegistrarOActualizar_NuevoRegistro() {
        when(afluenciaRepository.findByFechaAndFranjaHoraria(hoy, FranjaHoraria.TARDE_NOCHE))
                .thenReturn(Optional.empty());
        when(afluenciaRepository.save(any(RegistroAfluencia.class))).thenAnswer(i -> i.getArgument(0));

        RegistroAfluencia reg = afluenciaService.registrarOActualizar(hoy, FranjaHoraria.TARDE_NOCHE, 55);

        assertNotNull(reg);
        assertEquals(hoy, reg.getFecha());
        assertEquals(FranjaHoraria.TARDE_NOCHE, reg.getFranjaHoraria());
        assertEquals(55, reg.getConcurrenciaAprox());
        verify(afluenciaRepository, times(1)).save(any(RegistroAfluencia.class));
    }

    @Test
    @DisplayName("RF16 - Debe actualizar la concurrencia existente si ya hay un registro previo")
    void testRegistrarOActualizar_ActualizaExistente() {
        RegistroAfluencia existente = new RegistroAfluencia();
        existente.setId(20L);
        existente.setFecha(hoy);
        existente.setFranjaHoraria(FranjaHoraria.MANANA);
        existente.setConcurrenciaAprox(30);

        when(afluenciaRepository.findByFechaAndFranjaHoraria(hoy, FranjaHoraria.MANANA))
                .thenReturn(Optional.of(existente));
        when(afluenciaRepository.save(any(RegistroAfluencia.class))).thenAnswer(i -> i.getArgument(0));

        RegistroAfluencia actualizado = afluenciaService.registrarOActualizar(hoy, FranjaHoraria.MANANA, 42);

        assertNotNull(actualizado);
        assertEquals(42, actualizado.getConcurrenciaAprox(), "La concurrencia debió actualizarse a 42");
        assertEquals(20L, actualizado.getId(), "Debe conservar el mismo ID existente");
        verify(afluenciaRepository, times(1)).save(existente);
    }
}
