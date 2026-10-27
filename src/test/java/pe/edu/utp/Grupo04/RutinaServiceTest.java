package pe.edu.utp.Grupo04;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import pe.edu.utp.Grupo04.model.RutinaDia;
import pe.edu.utp.Grupo04.repository.*;
import pe.edu.utp.Grupo04.service.RutinaService;
import java.time.*;
import java.util.*;
import java.util.stream.IntStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RutinaServiceTest {
    private RutinaDia dia(int dia, String fecha, String sesion) {
        var d = new RutinaDia(); d.setDia(dia); d.setVigenteDesde(LocalDate.parse(fecha));
        d.setSesion(sesion); d.setDescripcion("Press: 3 × 10"); return d;
    }
    @Test void cambioDesdeJuevesConservaLunesAnterior() {
        var versiones = List.of(dia(1,"2026-10-01","Anterior"), dia(1,"2026-10-22","Nueva"),
                dia(4,"2026-10-01","Anterior"), dia(4,"2026-10-22","Nueva"));
        assertEquals("Anterior", RutinaService.resolver(versiones, LocalDate.parse("2026-10-19")).getSesion());
        assertEquals("Nueva", RutinaService.resolver(versiones, LocalDate.parse("2026-10-22")).getSesion());
        assertEquals("Nueva", RutinaService.resolver(versiones, LocalDate.parse("2026-10-26")).getSesion());
    }
    @Test void metaCuentaCalendarioRealYNoDescansos() {
        var versiones = List.of(dia(1,"2026-10-01","Pecho"),dia(3,"2026-10-01","Espalda"),dia(5,"2026-10-01","Pierna"));
        assertEquals(13, RutinaService.metaMes(versiones,LocalDate.parse("2026-10-15")));
        assertNull(RutinaService.resolver(versiones,LocalDate.parse("2026-10-20")));
    }
    @Test void primeraConfiguracionNoInventaSesionesPasadas() {
        var versiones = List.of(dia(1,"2026-10-22","Pecho"),dia(3,"2026-10-22","Espalda"),dia(5,"2026-10-22","Pierna"));
        assertEquals(4, RutinaService.metaMes(versiones,LocalDate.parse("2026-10-22")));
    }
    @Test void diasFuturosYDescansosNoSeRegistran() {
        var repo = mock(RutinaDiaRepository.class); var registros = mock(EntrenamientoRegistroRepository.class);
        var service = new RutinaService(repo, registros);
        var hoy = LocalDate.now(ZoneId.of("America/Lima"));
        assertThrows(ResponseStatusException.class, () -> service.registrar("ana",new RutinaService.RegistroInput(hoy.plusDays(1),true)));
        when(repo.findByUsernameOrderByVigenteDesdeAscDiaAsc("ana")).thenReturn(List.of());
        assertThrows(ResponseStatusException.class, () -> service.registrar("ana",new RutinaService.RegistroInput(hoy,true)));
        verifyNoInteractions(registros);
    }
    @Test void configuracionRequiereDiasUnicosYUnaSesion() {
        var repo = mock(RutinaDiaRepository.class); var registros = mock(EntrenamientoRegistroRepository.class);
        var service = new RutinaService(repo, registros);
        var descanso = IntStream.rangeClosed(1,7).mapToObj(i -> new RutinaService.DiaInput(i,"","")).toList();
        assertThrows(ResponseStatusException.class, () -> service.guardar("ana", descanso));
        assertThrows(ResponseStatusException.class, () -> service.guardar("ana", Collections.nCopies(7,new RutinaService.DiaInput(1,"Pecho","3 × 10"))));
        verifyNoInteractions(repo, registros);
    }
    @Test void resumenConsultaSoloRegistrosDelUsuarioActual() {
        var repo = mock(RutinaDiaRepository.class); var registros = mock(EntrenamientoRegistroRepository.class);
        when(repo.findByUsernameOrderByVigenteDesdeAscDiaAsc("ana")).thenReturn(List.of());
        when(registros.findByUsername("ana")).thenReturn(List.of());
        var resumen = new RutinaService(repo, registros).resumen("ana");
        assertEquals(0,resumen.metaMes()); assertEquals(0,resumen.completadosSemana());
        verify(repo).findByUsernameOrderByVigenteDesdeAscDiaAsc("ana"); verify(registros).findByUsername("ana");
    }
}
