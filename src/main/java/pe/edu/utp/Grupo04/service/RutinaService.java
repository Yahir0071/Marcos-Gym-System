package pe.edu.utp.Grupo04.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import pe.edu.utp.Grupo04.model.*;
import pe.edu.utp.Grupo04.repository.*;
import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

@Service
@Transactional
public class RutinaService {
    private final RutinaDiaRepository rutinas;
    private final EntrenamientoRegistroRepository registros;
    public RutinaService(RutinaDiaRepository rutinas, EntrenamientoRegistroRepository registros) {
        this.rutinas = rutinas; this.registros = registros;
    }
    public record DiaInput(int dia, String sesion, String descripcion) {}
    public record RegistroInput(LocalDate fecha, boolean completado) {}
    public record DiaVista(LocalDate fecha, String sesion, String descripcion, boolean completado) {}
    public record Resumen(LocalDate hoy, List<DiaVista> semana, List<DiaInput> rutina,
                          long completadosSemana, long metaSemana, long completadosMes, long metaMes, int racha) {}
    private LocalDate hoy() { return LocalDate.now(ZoneId.of("America/Lima")); }
    private static ResponseStatusException error(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
    // Cada fecha conserva la versión que estaba vigente ese día.
    public static RutinaDia resolver(List<RutinaDia> versiones, LocalDate fecha) {
        return versiones.stream().filter(d -> d.getDia() == fecha.getDayOfWeek().getValue()
                        && !d.getVigenteDesde().isAfter(fecha))
                .max(Comparator.comparing(RutinaDia::getVigenteDesde)).orElse(null);
    }
    private static boolean programado(RutinaDia d) { return d != null && !d.getSesion().isBlank(); }
    public static long metaMes(List<RutinaDia> versiones, LocalDate fecha) {
        return fecha.withDayOfMonth(1).datesUntil(fecha.withDayOfMonth(1).plusMonths(1))
                .filter(d -> programado(resolver(versiones, d))).count();
    }
    @Transactional(readOnly = true)
    public Resumen resumen(String username) { return resumen(username, hoy()); }
    private Resumen resumen(String username, LocalDate hoy) {
        var versiones = rutinas.findByUsernameOrderByVigenteDesdeAscDiaAsc(username);
        Set<LocalDate> hechos = new HashSet<>();
        registros.findByUsername(username).forEach(r -> hechos.add(r.getFecha()));
        var lunes = hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        var semana = lunes.datesUntil(lunes.plusDays(7)).map(fecha -> {
            var d = resolver(versiones, fecha);
            return new DiaVista(fecha, d == null ? "" : d.getSesion(), d == null ? "" : d.getDescripcion(), hechos.contains(fecha));
        }).toList();
        // El editor muestra la versión actual para los siete días, independientemente de esta semana.
        var vigente = versiones.stream().map(RutinaDia::getVigenteDesde).max(LocalDate::compareTo).orElse(hoy);
        var actual = versiones.stream().filter(d -> d.getVigenteDesde().equals(vigente))
                .map(d -> new DiaInput(d.getDia(), d.getSesion(), d.getDescripcion())).toList();
        long mesHechos = hechos.stream().filter(d -> YearMonth.from(d).equals(YearMonth.from(hoy))
                && programado(resolver(versiones, d))).count();
        int racha = 0;
        LocalDate inicio = versiones.stream().map(RutinaDia::getVigenteDesde).min(LocalDate::compareTo).orElse(hoy);
        for (var d = hoy; !d.isBefore(inicio); d = d.minusDays(1)) {
            if (!programado(resolver(versiones, d))) continue;
            if (hechos.contains(d)) racha++;
            else if (!d.equals(hoy)) break;
        }
        return new Resumen(hoy, semana, actual,
                semana.stream().filter(d -> !d.sesion().isBlank() && d.completado()).count(),
                semana.stream().filter(d -> !d.sesion().isBlank()).count(), mesHechos, metaMes(versiones, hoy), racha);
    }
    public Resumen guardar(String username, List<DiaInput> dias) {
        if (dias == null || dias.size() != 7 || dias.stream().anyMatch(Objects::isNull)
                || dias.stream().map(DiaInput::dia).distinct().count() != 7
                || dias.stream().anyMatch(d -> d.dia() < 1 || d.dia() > 7 || d.sesion() == null
                    || d.descripcion() == null || d.sesion().length() > 100 || d.descripcion().length() > 4000)
                || dias.stream().noneMatch(d -> !d.sesion().isBlank()))
            throw error("Configura los siete días y al menos un día de entrenamiento (sesión: hasta 100 caracteres; descripción: hasta 4000).");
        LocalDate hoy = hoy();
        var anteriores = rutinas.findByUsernameOrderByVigenteDesdeAscDiaAsc(username);
        for (var input : dias) {
            RutinaDia d = anteriores.stream().filter(v -> v.getVigenteDesde().equals(hoy) && v.getDia() == input.dia())
                    .findFirst().orElseGet(RutinaDia::new);
            d.setUsername(username); d.setVigenteDesde(hoy); d.setDia(input.dia());
            d.setSesion(input.sesion().trim()); d.setDescripcion(input.sesion().isBlank() ? "" : input.descripcion().trim());
            rutinas.save(d);
        }
        // Cambiar hoy a descanso no deja una sesión completada oculta.
        if (dias.stream().anyMatch(d -> d.dia() == hoy.getDayOfWeek().getValue() && d.sesion().isBlank()))
            registros.findByUsernameAndFecha(username, hoy).ifPresent(registros::delete);
        rutinas.flush(); registros.flush();
        return resumen(username, hoy);
    }
    public Resumen registrar(String username, RegistroInput input) {
        LocalDate hoy = hoy();
        if (input == null || input.fecha() == null || input.fecha().isAfter(hoy)
                || input.fecha().isBefore(hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))))
            throw error("Solo puedes registrar días transcurridos de la semana actual.");
        if (!programado(resolver(rutinas.findByUsernameOrderByVigenteDesdeAscDiaAsc(username), input.fecha())))
            throw error("Este día es de descanso o todavía no tenía una rutina configurada.");
        var existente = registros.findByUsernameAndFecha(username, input.fecha());
        if (input.completado() && existente.isEmpty()) {
            var r = new EntrenamientoRegistro(); r.setUsername(username); r.setFecha(input.fecha()); registros.save(r);
        } else if (!input.completado()) existente.ifPresent(registros::delete);
        registros.flush(); return resumen(username, hoy);
    }
}
