package pe.edu.utp.Grupo04.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo04.model.Membresia;
import pe.edu.utp.Grupo04.model.RegistroAcceso;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.model.enums.EstadoMembresia;
import pe.edu.utp.Grupo04.model.enums.FranjaHoraria;
import pe.edu.utp.Grupo04.repository.RegistroAccesoRepository;
import pe.edu.utp.Grupo04.service.AccesoService;
import pe.edu.utp.Grupo04.service.AfluenciaService;
import pe.edu.utp.Grupo04.service.CongelamientoService;
import pe.edu.utp.Grupo04.service.MembresiaService;
import pe.edu.utp.Grupo04.service.SocioService;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class AccesoServiceImpl implements AccesoService {

    public static final ZoneId ZONA_GIMNASIO = ZoneId.of("America/Lima");
    public static final int CAPACIDAD_MAXIMA = AfluenciaService.CAPACIDAD_MAXIMA;
    public static final int MINUTOS_EXPIRACION_QR = 5;
    public static final int MINUTOS_COOLDOWN_SOCIO = 60;
    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("hh:mm a");

    private final SocioService socioService;
    private final MembresiaService membresiaService;
    private final CongelamientoService congelamientoService;
    private final AfluenciaService afluenciaService;
    private final RegistroAccesoRepository registroAccesoRepository;
    private final Clock clock;
    private final boolean validarHorario;

    @Autowired
    public AccesoServiceImpl(SocioService socioService,
                             MembresiaService membresiaService,
                             CongelamientoService congelamientoService,
                             AfluenciaService afluenciaService,
                             RegistroAccesoRepository registroAccesoRepository,
                             @Value("${app.torniquete.validar-horario:true}") boolean validarHorario) {
        this(socioService, membresiaService, congelamientoService, afluenciaService,
                registroAccesoRepository, Clock.system(ZONA_GIMNASIO), validarHorario);
    }

    public AccesoServiceImpl(SocioService socioService,
                             MembresiaService membresiaService,
                             CongelamientoService congelamientoService,
                             AfluenciaService afluenciaService,
                             RegistroAccesoRepository registroAccesoRepository,
                             Clock clock,
                             boolean validarHorario) {
        this.socioService = socioService;
        this.membresiaService = membresiaService;
        this.congelamientoService = congelamientoService;
        this.afluenciaService = afluenciaService;
        this.registroAccesoRepository = registroAccesoRepository;
        this.clock = clock;
        this.validarHorario = validarHorario;
    }

    @Override
    public Map<String, Object> generarPaseQr(String username) {
        Socio socio = buscarSocio(username);
        if (socio == null) {
            return Map.of(
                    "success", false,
                    "mensaje", "Socio no registrado en el sistema."
            );
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        String token = "EG-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);

        RegistroAcceso acceso = new RegistroAcceso();
        acceso.setSocio(socio);
        acceso.setQrToken(token);
        acceso.setEstado("EMITIDO");
        acceso.setFechaEmision(ahora);
        acceso.setFechaExpiracion(ahora.plusMinutes(MINUTOS_EXPIRACION_QR));
        registroAccesoRepository.save(acceso);

        String contenidoQr = "ENERGY GYM SOCIO " + socio.getId() + " " + token;
        return Map.of(
                "success", true,
                "token", token,
                "contenidoQr", contenidoQr,
                "expiraEnSegundos", MINUTOS_EXPIRACION_QR * 60,
                "sede", "Sede Las Américas, Ate"
        );
    }

    @Override
    public Map<String, Object> validarAcceso(String username, String qrToken) {
        Socio socio = buscarSocio(username);
        if (socio == null) {
            return denegado("Acceso denegado: Socio no registrado en el sistema.");
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDate hoy = ahora.toLocalDate();

        RegistroAcceso pase = null;
        if (qrToken != null && !qrToken.isBlank()) {
            pase = registroAccesoRepository.findLockedByQrToken(qrToken.strip()).orElse(null);
            if (pase == null || !pase.getSocio().getId().equals(socio.getId())) {
                return denegado("Código QR inválido o no pertenece a tu cuenta.");
            }
            if (!"EMITIDO".equals(pase.getEstado())) {
                return denegado("Este código QR ya fue utilizado. Cierra y vuelve a abrir tu carnet para generar uno nuevo.");
            }
            if (ahora.isAfter(pase.getFechaExpiracion())) {
                pase.setEstado("EXPIRADO");
                pase.setFechaUso(ahora);
                pase.setMotivo("QR expirado");
                registroAccesoRepository.save(pase);
                return denegado("El código QR ha expirado. Cierra y vuelve a abrir tu carnet para generar uno nuevo.");
            }
        }

        if (validarHorario && !estaAbierto(ahora)) {
            return registrarDenegado(pase, ahora, "Fuera de horario de atención",
                    "Sede cerrada en este horario (Lun–Vie 06:00–23:00, Sáb 07:00–20:00, Dom 08:00–14:00).");
        }

        Optional<Membresia> memOpt = membresiaService.obtenerMembresiaActivaPorUsername(username)
                .filter(m -> m.getEstado() == EstadoMembresia.ACTIVA
                        && m.getFechaInicio() != null
                        && !m.getFechaInicio().isAfter(hoy)
                        && m.getFechaFin() != null
                        && !m.getFechaFin().isBefore(hoy));

        boolean congelada = memOpt.map(m -> congelamientoService.membresiaCongelada(m.getId(), hoy)).orElse(false);
        if (memOpt.isEmpty() || congelada) {
            String msg = congelada
                    ? "La membresía está en pausa en esta fecha."
                    : "Acceso denegado: El socio no cuenta con una membresía activa y vigente en el gimnasio.";
            return registrarDenegado(pase, ahora, congelada ? "Membresía congelada" : "Sin membresía activa", msg);
        }

        Optional<RegistroAcceso> ultimoIngreso = registroAccesoRepository
                .findTopBySocioIdAndEstadoOrderByFechaUsoDesc(socio.getId(), "PERMITIDO");
        if (ultimoIngreso.isPresent() && ultimoIngreso.get().getFechaUso() != null) {
            LocalDateTime finCooldown = ultimoIngreso.get().getFechaUso().plusMinutes(MINUTOS_COOLDOWN_SOCIO);
            if (finCooldown.isAfter(ahora)) {
                long minRestantes = Math.max(1, Duration.between(ahora, finCooldown).toMinutes());
                return registrarDenegado(pase, ahora, "Cooldown activo",
                        "Ya registraste un ingreso reciente. Espera " + minRestantes + " min antes de volver a escanear.");
            }
        }

        FranjaHoraria franja = determinarFranja(ahora.toLocalTime());
        int concurrenciaActual = afluenciaService.listarPorFecha(hoy).stream()
                .filter(r -> r.getFranjaHoraria() == franja && r.getConcurrenciaAprox() != null)
                .mapToInt(r -> Math.max(0, r.getConcurrenciaAprox()))
                .findFirst()
                .orElse(0);
        if (concurrenciaActual >= CAPACIDAD_MAXIMA) {
            return registrarDenegado(pase, ahora, "Aforo completo",
                    "Aforo completo en esta franja (" + CAPACIDAD_MAXIMA + "/" + CAPACIDAD_MAXIMA + ").");
        }

        int nuevaConcurrencia = afluenciaService.incrementarPorAcceso(hoy, franja, CAPACIDAD_MAXIMA);
        if (pase != null) {
            pase.setEstado("PERMITIDO");
            pase.setFranjaHoraria(franja);
            pase.setFechaUso(ahora);
            pase.setMotivo("Ingreso válido");
            registroAccesoRepository.save(pase);
        }

        String horaActual = ahora.toLocalTime().format(HORA_FMT);
        String etiquetaFranja = etiquetaFranja(franja);
        return Map.of(
                "success", true,
                "status", "CONCEDIDO",
                "mensaje", "Acceso concedido. Afluencia " + etiquetaFranja + ": " + nuevaConcurrencia + "/" + CAPACIDAD_MAXIMA + ".",
                "sede", "Sede Las Américas, Ate - Ingreso Central",
                "hora", horaActual,
                "franja", franja.name(),
                "concurrencia", nuevaConcurrencia,
                "capacidadMaxima", CAPACIDAD_MAXIMA
        );
    }

    public static boolean estaAbierto(LocalDateTime fechaHora) {
        DayOfWeek dia = fechaHora.getDayOfWeek();
        LocalTime hora = fechaHora.toLocalTime();
        return switch (dia) {
            case SATURDAY -> !hora.isBefore(LocalTime.of(7, 0)) && hora.isBefore(LocalTime.of(20, 0));
            case SUNDAY -> !hora.isBefore(LocalTime.of(8, 0)) && hora.isBefore(LocalTime.of(14, 0));
            default -> !hora.isBefore(LocalTime.of(6, 0)) && hora.isBefore(LocalTime.of(23, 0));
        };
    }

    public static FranjaHoraria determinarFranja(LocalTime hora) {
        if (hora.isBefore(LocalTime.of(11, 0))) {
            return FranjaHoraria.MANANA;
        }
        if (hora.isBefore(LocalTime.of(16, 0))) {
            return FranjaHoraria.MEDIODIA;
        }
        return FranjaHoraria.TARDE_NOCHE;
    }

    private Socio buscarSocio(String username) {
        if (username == null || username.isBlank() || "anonymousUser".equals(username)) {
            return null;
        }
        return socioService.buscarPorUsername(username).orElse(null);
    }

    private Map<String, Object> registrarDenegado(RegistroAcceso pase, LocalDateTime ahora, String motivo, String mensaje) {
        if (pase != null) {
            pase.setEstado("DENEGADO");
            pase.setFechaUso(ahora);
            pase.setMotivo(motivo);
            registroAccesoRepository.save(pase);
        }
        return denegado(mensaje);
    }

    private Map<String, Object> denegado(String mensaje) {
        return Map.of(
                "success", false,
                "status", "DENEGADO",
                "mensaje", mensaje,
                "sede", "Sede Las Américas, Ate"
        );
    }

    private static String etiquetaFranja(FranjaHoraria franja) {
        return switch (franja) {
            case MANANA -> "Mañana (06–11h)";
            case MEDIODIA -> "Mediodía (11–16h)";
            case TARDE_NOCHE -> "Tarde-Noche (16–23h)";
        };
    }
}
