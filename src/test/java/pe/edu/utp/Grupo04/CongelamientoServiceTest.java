package pe.edu.utp.Grupo04;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.utp.Grupo04.dto.SolicitudCongelamientoDTO;
import pe.edu.utp.Grupo04.model.Membresia;
import pe.edu.utp.Grupo04.model.Plan;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.model.SolicitudCongelamiento;
import pe.edu.utp.Grupo04.model.enums.EstadoMembresia;
import pe.edu.utp.Grupo04.model.enums.EstadoSolicitudCongelamiento;
import pe.edu.utp.Grupo04.repository.SolicitudCongelamientoRepository;
import pe.edu.utp.Grupo04.repository.MembresiaRepository;
import pe.edu.utp.Grupo04.repository.SocioRepository;
import pe.edu.utp.Grupo04.service.MembresiaService;
import pe.edu.utp.Grupo04.service.SocioService;
import pe.edu.utp.Grupo04.service.impl.CongelamientoServiceImpl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CongelamientoServiceTest {

    @Mock
    private SolicitudCongelamientoRepository congelamientoRepository;
    @Mock
    private SocioRepository socioRepository;
    @Mock
    private MembresiaRepository membresiaRepository;
    @Mock
    private SocioService socioService;
    @Mock
    private MembresiaService membresiaService;

    @InjectMocks
    private CongelamientoServiceImpl congelamientoService;

    private Socio socio;
    private Membresia membresiaActiva;

    @BeforeEach
    void setUp() {
        socio = new Socio();
        socio.setId(10L);
        socio.setDni("72563123");
        socio.setNombres("Carlos");
        socio.setApellidos("Mendoza");

        Plan plan = new Plan();
        plan.setId(1L);
        plan.setNombre("Plan Plus");

        membresiaActiva = new Membresia();
        membresiaActiva.setId(100L);
        membresiaActiva.setSocio(socio);
        membresiaActiva.setPlan(plan);
        membresiaActiva.setEstado(EstadoMembresia.ACTIVA);
        membresiaActiva.setFechaInicio(LocalDate.now().minusDays(5));
        membresiaActiva.setFechaFin(LocalDate.now().plusDays(25));
    }

    @Test
    @DisplayName("M08 - Registra solicitud persistente de congelamiento vinculada al socio autenticado")
    void testRegistrarSolicitud_Exitosa() {
        SolicitudCongelamientoDTO dto = new SolicitudCongelamientoDTO(
                7,
                LocalDate.now().plusDays(2),
                "Viaje laboral a provincia por una semana"
        );

        when(socioService.buscarPorUsername("carlos@energygym.pe")).thenReturn(Optional.of(socio));
        when(socioRepository.findLockedById(10L)).thenReturn(Optional.of(socio));
        when(membresiaService.obtenerMembresiaActivaPorUsername("carlos@energygym.pe")).thenReturn(Optional.of(membresiaActiva));
        when(congelamientoRepository.existsBySocioIdAndEstado(10L, EstadoSolicitudCongelamiento.PENDIENTE)).thenReturn(false);
        when(congelamientoRepository.save(any(SolicitudCongelamiento.class))).thenAnswer(inv -> {
            SolicitudCongelamiento s = inv.getArgument(0);
            s.setId(77L);
            return s;
        });

        SolicitudCongelamiento guardada = congelamientoService.registrarSolicitud("carlos@energygym.pe", dto);

        assertNotNull(guardada);
        assertEquals(77L, guardada.getId());
        assertEquals(7, guardada.getDiasSolicitados());
        assertEquals(EstadoSolicitudCongelamiento.PENDIENTE, guardada.getEstado());
        assertEquals(socio, guardada.getSocio());
        verify(congelamientoRepository, times(1)).save(any(SolicitudCongelamiento.class));
    }

    @Test
    @DisplayName("M08 - Rechaza congelamiento si el socio no tiene membresía activa")
    void testRegistrarSolicitud_SinMembresiaActiva() {
        SolicitudCongelamientoDTO dto = new SolicitudCongelamientoDTO(7, LocalDate.now(), "Descanso médico");
        when(socioService.buscarPorUsername("carlos@energygym.pe")).thenReturn(Optional.of(socio));
        when(socioRepository.findLockedById(10L)).thenReturn(Optional.of(socio));
        when(membresiaService.obtenerMembresiaActivaPorUsername("carlos@energygym.pe")).thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> congelamientoService.registrarSolicitud("carlos@energygym.pe", dto));

        assertTrue(ex.getMessage().contains("membresía activa"));
        verify(congelamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("M08 - Impide duplicar una solicitud pendiente del mismo socio")
    void testRegistrarSolicitud_DuplicadaPendiente() {
        SolicitudCongelamientoDTO dto = new SolicitudCongelamientoDTO(15, LocalDate.now().plusDays(1), "Viaje familiar");
        when(socioService.buscarPorUsername("carlos@energygym.pe")).thenReturn(Optional.of(socio));
        when(socioRepository.findLockedById(10L)).thenReturn(Optional.of(socio));
        when(membresiaService.obtenerMembresiaActivaPorUsername("carlos@energygym.pe")).thenReturn(Optional.of(membresiaActiva));
        when(congelamientoRepository.existsBySocioIdAndEstado(10L, EstadoSolicitudCongelamiento.PENDIENTE)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> congelamientoService.registrarSolicitud("carlos@energygym.pe", dto));

        assertTrue(ex.getMessage().contains("pendiente"));
        verify(congelamientoRepository, never()).save(any());
    }

    @Test
    @DisplayName("M08 - Rechaza fecha de inicio en el pasado")
    void testRegistrarSolicitud_FechaPasada() {
        SolicitudCongelamientoDTO dto = new SolicitudCongelamientoDTO(7, LocalDate.now().minusDays(1), "Motivo válido");
        when(socioService.buscarPorUsername("carlos@energygym.pe")).thenReturn(Optional.of(socio));
        when(socioRepository.findLockedById(10L)).thenReturn(Optional.of(socio));
        when(membresiaService.obtenerMembresiaActivaPorUsername("carlos@energygym.pe")).thenReturn(Optional.of(membresiaActiva));

        assertThrows(IllegalArgumentException.class,
                () -> congelamientoService.registrarSolicitud("carlos@energygym.pe", dto));
        verify(congelamientoRepository, never()).save(any());
    }

    @Test
    void aprobarExtiendeMembresiaUnaSolaVezYRegistraResponsable() {
        LocalDate inicioPausa = LocalDate.now(ZoneId.of("America/Lima"));
        LocalDate finOriginal = membresiaActiva.getFechaFin();
        var solicitud = new SolicitudCongelamiento();
        solicitud.setSocio(socio);
        solicitud.setDiasSolicitados(7);
        solicitud.setFechaInicioSolicitada(inicioPausa);
        var titular = mock(SolicitudCongelamientoRepository.Titular.class);
        when(titular.getMembresiaId()).thenReturn(100L);
        when(titular.getSocioId()).thenReturn(10L);
        when(congelamientoRepository.buscarTitular(44L)).thenReturn(Optional.of(titular));
        when(socioRepository.findLockedById(10L)).thenReturn(Optional.of(socio));
        when(membresiaRepository.buscarParaActualizar(100L)).thenReturn(Optional.of(membresiaActiva));
        when(congelamientoRepository.findById(44L)).thenReturn(Optional.of(solicitud));
        var siguiente = new Membresia();
        siguiente.setEstado(EstadoMembresia.PENDIENTE);
        siguiente.setFechaInicio(finOriginal.plusDays(1));
        siguiente.setFechaFin(finOriginal.plusDays(31));
        when(membresiaRepository.findBySocioId(10L)).thenReturn(List.of(membresiaActiva, siguiente));

        congelamientoService.resolver(44L, EstadoSolicitudCongelamiento.APROBADA, "recepcion@example.test");
        assertEquals(EstadoSolicitudCongelamiento.APROBADA, solicitud.getEstado());
        assertEquals("recepcion@example.test", solicitud.getResueltoPor());
        assertNotNull(solicitud.getFechaResolucion());
        assertEquals(finOriginal.plusDays(7), membresiaActiva.getFechaFin());
        assertEquals(finOriginal.plusDays(8), siguiente.getFechaInicio());
        assertEquals(finOriginal.plusDays(38), siguiente.getFechaFin());

        assertThrows(IllegalStateException.class,
                () -> congelamientoService.resolver(44L, EstadoSolicitudCongelamiento.APROBADA, "recepcion@example.test"));
        assertEquals(finOriginal.plusDays(7), membresiaActiva.getFechaFin());
    }

    @Test
    @DisplayName("M08 - Lista únicamente las solicitudes del socio autenticado")
    void testListarPorUsername_DevuelveSolicitudesPropias() {
        SolicitudCongelamiento s = new SolicitudCongelamiento();
        s.setId(1L);
        s.setSocio(socio);

        when(socioService.buscarPorUsername("carlos@energygym.pe")).thenReturn(Optional.of(socio));
        when(congelamientoRepository.findBySocioIdOrderByFechaSolicitudDesc(10L)).thenReturn(List.of(s));

        List<SolicitudCongelamiento> lista = congelamientoService.listarPorUsername("carlos@energygym.pe");

        assertEquals(1, lista.size());
        assertEquals(1L, lista.get(0).getId());
    }
}
