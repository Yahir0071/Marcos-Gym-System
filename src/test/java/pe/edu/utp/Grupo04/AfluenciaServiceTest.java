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
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
        var esperado = new RegistroAfluencia();
        esperado.setFecha(hoy);
        esperado.setFranjaHoraria(FranjaHoraria.TARDE_NOCHE);
        esperado.setConcurrenciaAprox(55);
        when(afluenciaRepository.findByFechaAndFranjaHoraria(hoy, FranjaHoraria.TARDE_NOCHE))
                .thenReturn(Optional.of(esperado));

        RegistroAfluencia reg = afluenciaService.registrarOActualizar(hoy, FranjaHoraria.TARDE_NOCHE, 55);

        assertNotNull(reg);
        assertEquals(hoy, reg.getFecha());
        assertEquals(FranjaHoraria.TARDE_NOCHE, reg.getFranjaHoraria());
        assertEquals(55, reg.getConcurrenciaAprox());
        verify(afluenciaRepository).registrarOActualizar(eq(hoy), eq("TARDE_NOCHE"), eq(55), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("RF16 - Debe actualizar la concurrencia existente si ya hay un registro previo")
    void testRegistrarOActualizar_ActualizaExistente() {
        RegistroAfluencia existente = new RegistroAfluencia();
        existente.setId(20L);
        existente.setFecha(hoy);
        existente.setFranjaHoraria(FranjaHoraria.MANANA);
        existente.setConcurrenciaAprox(42);

        when(afluenciaRepository.findByFechaAndFranjaHoraria(hoy, FranjaHoraria.MANANA))
                .thenReturn(Optional.of(existente));

        RegistroAfluencia actualizado = afluenciaService.registrarOActualizar(hoy, FranjaHoraria.MANANA, 42);

        assertNotNull(actualizado);
        assertEquals(42, actualizado.getConcurrenciaAprox(), "La concurrencia debió actualizarse a 42");
        assertEquals(20L, actualizado.getId(), "Debe conservar el mismo ID existente");
        verify(afluenciaRepository).registrarOActualizar(eq(hoy), eq("MANANA"), eq(42), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("M07 - Sin registros de afluencia, obtenerUltimaFechaActualizacion devuelve null (sin inventar fecha actual)")
    void testObtenerUltimaFechaActualizacion_SinRegistrosDevuelveNull() {
        when(afluenciaRepository.findTopByOrderByFechaRegistroDesc()).thenReturn(Optional.empty());

        LocalDateTime resultado = afluenciaService.obtenerUltimaFechaActualizacion();

        assertNull(resultado, "No debe fabricar LocalDateTime.now() cuando no hay registros");
    }

    @Test
    @DisplayName("M07 / M13 - Sin registros hoy, consulta únicamente la fecha más reciente sin llamar findAll()")
    void testObtenerAfluenciaVigente_UsaUltimaFechaSinFindAll() {
        LocalDate ayer = hoy.minusDays(1);
        RegistroAfluencia regAyer = new RegistroAfluencia();
        regAyer.setFecha(ayer);
        regAyer.setFranjaHoraria(FranjaHoraria.TARDE_NOCHE);
        regAyer.setConcurrenciaAprox(48);

        when(afluenciaRepository.findByFecha(hoy)).thenReturn(Collections.emptyList());
        when(afluenciaRepository.findUltimaFechaRegistrada()).thenReturn(Optional.of(ayer));
        when(afluenciaRepository.findByFecha(ayer)).thenReturn(List.of(regAyer));

        Map<FranjaHoraria, Integer> mapa = afluenciaService.obtenerAfluenciaVigente();

        assertEquals(0, mapa.get(FranjaHoraria.MANANA));
        assertEquals(0, mapa.get(FranjaHoraria.MEDIODIA));
        assertEquals(48, mapa.get(FranjaHoraria.TARDE_NOCHE));
        verify(afluenciaRepository, never()).findAll();
        verify(afluenciaRepository, times(1)).findUltimaFechaRegistrada();
    }
}
