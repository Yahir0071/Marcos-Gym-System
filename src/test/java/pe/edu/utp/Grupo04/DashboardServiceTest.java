package pe.edu.utp.Grupo04;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.utp.Grupo04.model.Membresia;
import pe.edu.utp.Grupo04.model.Plan;
import pe.edu.utp.Grupo04.model.RegistroAfluencia;
import pe.edu.utp.Grupo04.model.enums.EstadoEquipo;
import pe.edu.utp.Grupo04.model.enums.EstadoMembresia;
import pe.edu.utp.Grupo04.model.enums.FranjaHoraria;
import pe.edu.utp.Grupo04.repository.MembresiaRepository;
import pe.edu.utp.Grupo04.repository.PagoRepository;
import pe.edu.utp.Grupo04.repository.RegistroAfluenciaRepository;
import pe.edu.utp.Grupo04.service.EquipoService;
import pe.edu.utp.Grupo04.service.MembresiaService;
import pe.edu.utp.Grupo04.service.ReservaService;
import pe.edu.utp.Grupo04.service.SocioService;
import pe.edu.utp.Grupo04.service.impl.DashboardServiceImpl;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DashboardServiceTest {

    @Mock
    private SocioService socioService;
    @Mock
    private MembresiaService membresiaService;
    @Mock
    private ReservaService reservaService;
    @Mock
    private EquipoService equipoService;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private RegistroAfluenciaRepository afluenciaRepository;
    @Mock
    private MembresiaRepository membresiaRepository;

    private DashboardServiceImpl dashboardService;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        // 2026-11-18 (Miércoles) en zona America/Lima
        ZoneId zone = ZoneId.of("America/Lima");
        fixedClock = Clock.fixed(Instant.parse("2026-11-18T15:00:00Z"), zone);
        dashboardService = new DashboardServiceImpl(
                socioService,
                membresiaService,
                reservaService,
                equipoService,
                pagoRepository,
                afluenciaRepository,
                membresiaRepository
        );
    }

    @Test
    @DisplayName("M07 - Sin pagos en el mes debe mostrar 0.00 (nunca 16450.00 prefijado)")
    void testObtenerMetricasDashboard_SinPagosRetornaCero() {
        when(socioService.contarTotalSocios()).thenReturn(0L);
        when(membresiaRepository.findByEstado(EstadoMembresia.ACTIVA)).thenReturn(Collections.emptyList());
        when(membresiaService.listarProximasAVencer(7)).thenReturn(Collections.emptyList());
        when(reservaService.contarPendientes()).thenReturn(0L);
        when(equipoService.contarPorEstado(any(EstadoEquipo.class))).thenReturn(0L);
        when(pagoRepository.sumarMontoEnPeriodo(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0.0);
        when(pagoRepository.countByFechaPagoGreaterThanEqualAndFechaPagoLessThan(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(0L);
        when(afluenciaRepository.findByFechaBetweenOrderByFechaAsc(any(LocalDate.class), any(LocalDate.class))).thenReturn(Collections.emptyList());

        Map<String, Object> metricas = dashboardService.obtenerMetricasDashboard();

        assertEquals(0.0, metricas.get("ingresosMes"), "Sin pagos el ingreso mensual debe ser 0.0");
        assertEquals(false, metricas.get("tienePagosMes"));
        assertEquals(0L, metricas.get("cantidadPagosMes"));
        assertEquals(0.0, metricas.get("tasaSociosActivos"));
        assertEquals(false, metricas.get("tieneAfluenciaSemana"));
        assertTrue(((List<?>) metricas.get("distribucionPlanes")).isEmpty());
        verify(pagoRepository, never()).findAll();
        verify(afluenciaRepository, never()).findAll();
    }

    @Test
    @DisplayName("M07 / M13 - Filtra pagos estrictamente por el mes calendario actual y consolida afluencia en 1 sola consulta")
    void testObtenerMetricasDashboard_FiltraMesActualYDatosReales() {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioEsperado = hoy.withDayOfMonth(1).atStartOfDay();
        LocalDateTime finEsperado = hoy.withDayOfMonth(1).plusMonths(1).atStartOfDay();

        Plan planPlus = new Plan();
        planPlus.setId(1L);
        planPlus.setNombre("Plan Plus");
        planPlus.setPrecio(119.0);

        Membresia m1 = new Membresia();
        m1.setPlan(planPlus);
        m1.setEstado(EstadoMembresia.ACTIVA);

        RegistroAfluencia regHoy = new RegistroAfluencia();
        regHoy.setFecha(hoy);
        regHoy.setFranjaHoraria(FranjaHoraria.TARDE_NOCHE);
        regHoy.setConcurrenciaAprox(42);

        when(socioService.contarTotalSocios()).thenReturn(2L);
        when(membresiaRepository.findByEstado(EstadoMembresia.ACTIVA)).thenReturn(List.of(m1));
        when(membresiaService.listarProximasAVencer(7)).thenReturn(List.of(m1));
        when(reservaService.contarPendientes()).thenReturn(3L);
        when(equipoService.contarPorEstado(EstadoEquipo.OPERATIVO)).thenReturn(8L);
        when(equipoService.contarPorEstado(EstadoEquipo.EN_MANTENIMIENTO)).thenReturn(1L);
        when(equipoService.contarPorEstado(EstadoEquipo.FUERA_DE_SERVICIO)).thenReturn(1L);
        when(pagoRepository.sumarMontoEnPeriodo(eq(inicioEsperado), eq(finEsperado))).thenReturn(357.0);
        when(pagoRepository.countByFechaPagoGreaterThanEqualAndFechaPagoLessThan(eq(inicioEsperado), eq(finEsperado))).thenReturn(3L);
        when(afluenciaRepository.findByFechaBetweenOrderByFechaAsc(any(LocalDate.class), any(LocalDate.class))).thenReturn(List.of(regHoy));

        Map<String, Object> metricas = dashboardService.obtenerMetricasDashboard();

        assertEquals(357.0, metricas.get("ingresosMes"));
        assertEquals(true, metricas.get("tienePagosMes"));
        assertEquals(3L, metricas.get("cantidadPagosMes"));
        assertEquals(10L, metricas.get("totalEquipos"));
        assertEquals(50.0, metricas.get("tasaSociosActivos"));
        assertEquals(true, metricas.get("tieneAfluenciaSemana"));
        assertEquals(1, ((List<?>) metricas.get("distribucionPlanes")).size());

        verify(pagoRepository, times(1)).sumarMontoEnPeriodo(eq(inicioEsperado), eq(finEsperado));
        verify(afluenciaRepository, times(1)).findByFechaBetweenOrderByFechaAsc(any(LocalDate.class), any(LocalDate.class));
        verify(afluenciaRepository, never()).findAll();
    }
}
