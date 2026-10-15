package pe.edu.utp.Grupo04;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.utp.Grupo04.model.Promocion;
import pe.edu.utp.Grupo04.repository.PromocionRepository;
import pe.edu.utp.Grupo04.service.impl.PromocionServiceImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PromocionServiceTest {

    @Mock
    private PromocionRepository promocionRepository;

    @InjectMocks
    private PromocionServiceImpl promocionService;

    private Promocion promoVigente;
    private Promocion promoVencida;

    @BeforeEach
    void setUp() {
        LocalDate hoy = LocalDate.now();

        promoVigente = new Promocion();
        promoVigente.setId(1L);
        promoVigente.setTitulo("2x1 en Plan Plus");
        promoVigente.setPorcentajeDescuento(50.0);
        promoVigente.setFechaInicio(hoy.minusDays(5));
        promoVigente.setFechaFin(hoy.plusDays(10));
        promoVigente.setActiva(true);

        promoVencida = new Promocion();
        promoVencida.setId(2L);
        promoVencida.setTitulo("Black Friday Pasado");
        promoVencida.setPorcentajeDescuento(30.0);
        promoVencida.setFechaInicio(hoy.minusDays(30));
        promoVencida.setFechaFin(hoy.minusDays(5));
        promoVencida.setActiva(true);
    }

    @Test
    @DisplayName("RF15 - Debe filtrar y listar únicamente promociones activas cuya vigencia cubra la fecha actual")
    void testListarVigentes() {
        when(promocionRepository.findByActivaTrue()).thenReturn(List.of(promoVigente, promoVencida));

        List<Promocion> vigentes = promocionService.listarVigentes();

        assertNotNull(vigentes);
        assertEquals(1, vigentes.size(), "Solo debe retornar la promoción no vencida");
        assertEquals("2x1 en Plan Plus", vigentes.get(0).getTitulo());
        verify(promocionRepository, times(1)).findByActivaTrue();
    }

    @Test
    @DisplayName("RF15 - Debe cambiar el estado de activa/pausada correctamente")
    void testCambiarEstado() {
        when(promocionRepository.findById(1L)).thenReturn(Optional.of(promoVigente));
        when(promocionRepository.save(any(Promocion.class))).thenAnswer(i -> i.getArgument(0));

        promocionService.cambiarEstado(1L, false);

        assertFalse(promoVigente.getActiva(), "El estado debe haber cambiado a false");
        verify(promocionRepository, times(1)).save(promoVigente);
    }
}
