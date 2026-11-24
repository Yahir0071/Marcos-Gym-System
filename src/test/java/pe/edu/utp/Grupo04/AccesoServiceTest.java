package pe.edu.utp.Grupo04;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.utp.Grupo04.model.Membresia;
import pe.edu.utp.Grupo04.model.RegistroAcceso;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.model.enums.EstadoMembresia;
import pe.edu.utp.Grupo04.model.enums.FranjaHoraria;
import pe.edu.utp.Grupo04.repository.RegistroAccesoRepository;
import pe.edu.utp.Grupo04.service.AfluenciaService;
import pe.edu.utp.Grupo04.service.CongelamientoService;
import pe.edu.utp.Grupo04.service.MembresiaService;
import pe.edu.utp.Grupo04.service.SocioService;
import pe.edu.utp.Grupo04.service.impl.AccesoServiceImpl;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccesoServiceTest {

    @Mock
    private SocioService socioService;
    @Mock
    private MembresiaService membresiaService;
    @Mock
    private CongelamientoService congelamientoService;
    @Mock
    private AfluenciaService afluenciaService;
    @Mock
    private RegistroAccesoRepository registroAccesoRepository;

    private Socio socio;
    private Membresia membresiaActiva;

    // Lunes 2026-11-23 10:30 America/Lima (15:30 UTC) -> MANANA
    private final Clock relojLunesManana = Clock.fixed(
            Instant.parse("2026-11-23T15:30:00Z"),
            AccesoServiceImpl.ZONA_GIMNASIO
    );

    @BeforeEach
    void setUp() {
        socio = new Socio();
        socio.setId(7L);
        socio.setNombres("Carlos");
        socio.setApellidos("Mendoza");

        membresiaActiva = new Membresia();
        membresiaActiva.setId(15L);
        membresiaActiva.setSocio(socio);
        membresiaActiva.setEstado(EstadoMembresia.ACTIVA);
        membresiaActiva.setFechaInicio(LocalDate.of(2026, 11, 1));
        membresiaActiva.setFechaFin(LocalDate.of(2026, 12, 1));
    }

    @Test
    @DisplayName("Emite un QR único asociado al socio")
    void generarPaseQr_Exitoso() {
        AccesoServiceImpl service = crearServicio(relojLunesManana, true);
        when(socioService.buscarPorUsername("carlos@gym.pe")).thenReturn(Optional.of(socio));
        when(registroAccesoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Map<String, Object> res = service.generarPaseQr("carlos@gym.pe");

        assertEquals(true, res.get("success"));
        String contenido = (String) res.get("contenidoQr");
        assertTrue(contenido.startsWith("ENERGY GYM SOCIO 7 EG-"));
    }

    @Test
    @DisplayName("Concede acceso dentro de horario y suma +1 a la afluencia de la franja")
    void validarAcceso_ConcedeYSumaAfluencia() {
        AccesoServiceImpl service = crearServicio(relojLunesManana, true);
        LocalDateTime ahora = LocalDateTime.now(relojLunesManana);

        RegistroAcceso pase = new RegistroAcceso();
        pase.setSocio(socio);
        pase.setQrToken("EG-ABC123");
        pase.setEstado("EMITIDO");
        pase.setFechaEmision(ahora.minusMinutes(1));
        pase.setFechaExpiracion(ahora.plusMinutes(4));

        when(socioService.buscarPorUsername("carlos@gym.pe")).thenReturn(Optional.of(socio));
        when(registroAccesoRepository.findLockedByQrToken("EG-ABC123")).thenReturn(Optional.of(pase));
        when(membresiaService.obtenerMembresiaActivaPorUsername("carlos@gym.pe")).thenReturn(Optional.of(membresiaActiva));
        when(congelamientoService.membresiaCongelada(15L, ahora.toLocalDate())).thenReturn(false);
        when(registroAccesoRepository.findTopBySocioIdAndEstadoOrderByFechaUsoDesc(7L, "PERMITIDO")).thenReturn(Optional.empty());
        when(afluenciaService.listarPorFecha(ahora.toLocalDate())).thenReturn(List.of());
        when(afluenciaService.incrementarPorAcceso(ahora.toLocalDate(), FranjaHoraria.MANANA, 80)).thenReturn(1);

        Map<String, Object> res = service.validarAcceso("carlos@gym.pe", "EG-ABC123");

        assertEquals(true, res.get("success"));
        assertEquals("CONCEDIDO", res.get("status"));
        assertEquals("PERMITIDO", pase.getEstado());
        assertEquals(1, res.get("concurrencia"));
        verify(afluenciaService).incrementarPorAcceso(ahora.toLocalDate(), FranjaHoraria.MANANA, 80);
    }

    @Test
    @DisplayName("Rechaza un código QR ya utilizado sin sumar afluencia")
    void validarAcceso_RechazaQrReutilizado() {
        AccesoServiceImpl service = crearServicio(relojLunesManana, true);
        RegistroAcceso paseUsado = new RegistroAcceso();
        paseUsado.setSocio(socio);
        paseUsado.setQrToken("EG-USADO");
        paseUsado.setEstado("PERMITIDO");

        when(socioService.buscarPorUsername("carlos@gym.pe")).thenReturn(Optional.of(socio));
        when(registroAccesoRepository.findLockedByQrToken("EG-USADO")).thenReturn(Optional.of(paseUsado));

        Map<String, Object> res = service.validarAcceso("carlos@gym.pe", "EG-USADO");

        assertFalse((Boolean) res.get("success"));
        assertEquals("DENEGADO", res.get("status"));
        verify(afluenciaService, never()).incrementarPorAcceso(any(), any(), anyInt());
    }

    @Test
    @DisplayName("Rechaza nuevo ingreso si el socio está dentro del cooldown de 60 minutos")
    void validarAcceso_RechazaCooldown() {
        AccesoServiceImpl service = crearServicio(relojLunesManana, true);
        LocalDateTime ahora = LocalDateTime.now(relojLunesManana);

        RegistroAcceso paseNuevo = new RegistroAcceso();
        paseNuevo.setSocio(socio);
        paseNuevo.setQrToken("EG-NUEVO");
        paseNuevo.setEstado("EMITIDO");
        paseNuevo.setFechaEmision(ahora.minusMinutes(1));
        paseNuevo.setFechaExpiracion(ahora.plusMinutes(4));

        RegistroAcceso ingresoPrevio = new RegistroAcceso();
        ingresoPrevio.setEstado("PERMITIDO");
        ingresoPrevio.setFechaUso(ahora.minusMinutes(15));

        when(socioService.buscarPorUsername("carlos@gym.pe")).thenReturn(Optional.of(socio));
        when(registroAccesoRepository.findLockedByQrToken("EG-NUEVO")).thenReturn(Optional.of(paseNuevo));
        when(membresiaService.obtenerMembresiaActivaPorUsername("carlos@gym.pe")).thenReturn(Optional.of(membresiaActiva));
        when(congelamientoService.membresiaCongelada(15L, ahora.toLocalDate())).thenReturn(false);
        when(registroAccesoRepository.findTopBySocioIdAndEstadoOrderByFechaUsoDesc(7L, "PERMITIDO"))
                .thenReturn(Optional.of(ingresoPrevio));

        Map<String, Object> res = service.validarAcceso("carlos@gym.pe", "EG-NUEVO");

        assertFalse((Boolean) res.get("success"));
        assertEquals("DENEGADO", res.get("status"));
        assertEquals("DENEGADO", paseNuevo.getEstado());
        verify(afluenciaService, never()).incrementarPorAcceso(any(), any(), anyInt());
    }

    @Test
    @DisplayName("Rechaza acceso fuera del horario del gimnasio (ej. domingo 16:00)")
    void validarAcceso_RechazaFueraDeHorario() {
        // Domingo 2026-11-22 16:00 America/Lima (21:00 UTC) - Domingo cierra a las 14:00
        Clock relojDomingoTarde = Clock.fixed(Instant.parse("2026-11-22T21:00:00Z"), AccesoServiceImpl.ZONA_GIMNASIO);
        AccesoServiceImpl service = crearServicio(relojDomingoTarde, true);

        when(socioService.buscarPorUsername("carlos@gym.pe")).thenReturn(Optional.of(socio));

        Map<String, Object> res = service.validarAcceso("carlos@gym.pe", null);

        assertFalse((Boolean) res.get("success"));
        assertEquals("DENEGADO", res.get("status"));
        assertTrue(((String) res.get("mensaje")).contains("Sede cerrada"));
        verify(afluenciaService, never()).incrementarPorAcceso(any(), any(), anyInt());
    }

    private AccesoServiceImpl crearServicio(Clock clock, boolean validarHorario) {
        return new AccesoServiceImpl(
                socioService,
                membresiaService,
                congelamientoService,
                afluenciaService,
                registroAccesoRepository,
                clock,
                validarHorario
        );
    }
}
