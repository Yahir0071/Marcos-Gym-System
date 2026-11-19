package pe.edu.utp.Grupo04.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo04.model.Membresia;
import pe.edu.utp.Grupo04.model.RegistroAfluencia;
import pe.edu.utp.Grupo04.model.enums.EstadoEquipo;
import pe.edu.utp.Grupo04.model.enums.EstadoMembresia;
import pe.edu.utp.Grupo04.repository.MembresiaRepository;
import pe.edu.utp.Grupo04.repository.PagoRepository;
import pe.edu.utp.Grupo04.repository.RegistroAfluenciaRepository;
import pe.edu.utp.Grupo04.service.DashboardService;
import pe.edu.utp.Grupo04.service.EquipoService;
import pe.edu.utp.Grupo04.service.MembresiaService;
import pe.edu.utp.Grupo04.service.ReservaService;
import pe.edu.utp.Grupo04.service.SocioService;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final Locale LOCALE_PE = Locale.forLanguageTag("es-PE");
    private static final String[] DIAS_CORTOS = {"LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM"};

    private final SocioService socioService;
    private final MembresiaService membresiaService;
    private final ReservaService reservaService;
    private final EquipoService equipoService;
    private final PagoRepository pagoRepository;
    private final RegistroAfluenciaRepository afluenciaRepository;
    private final MembresiaRepository membresiaRepository;
    private final Clock clock;

    @Autowired
    public DashboardServiceImpl(SocioService socioService,
                                MembresiaService membresiaService,
                                ReservaService reservaService,
                                EquipoService equipoService,
                                PagoRepository pagoRepository,
                                RegistroAfluenciaRepository afluenciaRepository,
                                MembresiaRepository membresiaRepository) {
        this(socioService, membresiaService, reservaService, equipoService,
                pagoRepository, afluenciaRepository, membresiaRepository, Clock.systemDefaultZone());
    }

    DashboardServiceImpl(SocioService socioService,
                         MembresiaService membresiaService,
                         ReservaService reservaService,
                         EquipoService equipoService,
                         PagoRepository pagoRepository,
                         RegistroAfluenciaRepository afluenciaRepository,
                         MembresiaRepository membresiaRepository,
                         Clock clock) {
        this.socioService = socioService;
        this.membresiaService = membresiaService;
        this.reservaService = reservaService;
        this.equipoService = equipoService;
        this.pagoRepository = pagoRepository;
        this.afluenciaRepository = afluenciaRepository;
        this.membresiaRepository = membresiaRepository;
        this.clock = clock;
    }

    @Override
    public Map<String, Object> obtenerMetricasDashboard() {
        Map<String, Object> metricas = new HashMap<>();

        LocalDate hoy = LocalDate.now(clock);
        YearMonth mesActual = YearMonth.from(hoy);
        LocalDate inicioMesDate = mesActual.atDay(1);
        LocalDate finMesDate = mesActual.atEndOfMonth();
        LocalDateTime inicioMes = inicioMesDate.atStartOfDay();
        LocalDateTime inicioSiguienteMes = mesActual.plusMonths(1).atDay(1).atStartOfDay();

        long totalSocios = socioService.contarTotalSocios();
        List<Membresia> membresiasActivasLista = membresiaRepository.findByEstado(EstadoMembresia.ACTIVA);
        long membresiasActivas = membresiasActivasLista.size();
        List<Membresia> proximasAVencerLista = membresiaService.listarProximasAVencer(7);
        long reservasPendientes = reservaService.contarPendientes();

        long equiposOperativos = equipoService.contarPorEstado(EstadoEquipo.OPERATIVO);
        long equiposEnMantenimiento = equipoService.contarPorEstado(EstadoEquipo.EN_MANTENIMIENTO);
        long equiposFueraServicio = equipoService.contarPorEstado(EstadoEquipo.FUERA_DE_SERVICIO);
        long totalEquipos = equiposOperativos + equiposEnMantenimiento + equiposFueraServicio;

        Double sumaMes = pagoRepository.sumarMontoEnPeriodo(inicioMes, inicioSiguienteMes);
        double ingresosMes = sumaMes != null ? sumaMes : 0.0;
        long cantidadPagosMes = pagoRepository.countByFechaPagoGreaterThanEqualAndFechaPagoLessThan(inicioMes, inicioSiguienteMes);

        String mesNombre = mesActual.getMonth().getDisplayName(TextStyle.FULL, LOCALE_PE);
        String periodoMesLabel = Character.toUpperCase(mesNombre.charAt(0)) + mesNombre.substring(1) + " " + mesActual.getYear();

        metricas.put("totalSocios", totalSocios);
        metricas.put("membresiasActivas", membresiasActivas);
        metricas.put("membresiasPorVencer", proximasAVencerLista.size());
        metricas.put("proximasAVencerLista", proximasAVencerLista);
        metricas.put("reservasPendientes", reservasPendientes);
        metricas.put("equiposOperativos", equiposOperativos);
        metricas.put("equiposEnMantenimiento", equiposEnMantenimiento);
        metricas.put("equiposFueraServicio", equiposFueraServicio);
        metricas.put("totalEquipos", totalEquipos);

        // M07: Sin pagos prefijados — solo suma real filtrada al mes calendario vigente
        metricas.put("ingresosMes", ingresosMes);
        metricas.put("cantidadPagosMes", cantidadPagosMes);
        metricas.put("tienePagosMes", cantidadPagosMes > 0 && ingresosMes > 0.0);
        metricas.put("periodoMesLabel", periodoMesLabel);
        metricas.put("inicioMes", inicioMesDate);
        metricas.put("finMes", finMesDate);

        // M07 / M13: Afluencia semanal y mensual desde una sola consulta al rango más amplio
        LocalDate inicioSemana = hoy.with(DayOfWeek.MONDAY);
        LocalDate finSemana = inicioSemana.plusDays(6);
        LocalDate rangoInicio = inicioSemana.isBefore(inicioMesDate) ? inicioSemana : inicioMesDate;
        LocalDate rangoFin = finSemana.isAfter(finMesDate) ? finSemana : finMesDate;
        List<RegistroAfluencia> registrosRango = afluenciaRepository.findByFechaBetweenOrderByFechaAsc(rangoInicio, rangoFin);

        List<Map<String, Object>> flujoSemanal = construirBarrasPorDia(registrosRango, inicioSemana, finSemana);
        List<Map<String, Object>> flujoMensual = construirBarrasPorDia(registrosRango, inicioMesDate, finMesDate);
        boolean tieneAfluenciaSemana = flujoSemanal.stream().anyMatch(b -> ((Integer) b.get("valor")) > 0);
        boolean tieneAfluenciaMes = flujoMensual.stream().anyMatch(b -> ((Integer) b.get("valor")) > 0);

        metricas.put("flujoSemanal", flujoSemanal);
        metricas.put("flujoMensual", flujoMensual);
        metricas.put("flujoSemanalJson", serializarBarrasJson(flujoSemanal));
        metricas.put("flujoMensualJson", serializarBarrasJson(flujoMensual));
        metricas.put("tieneAfluenciaSemana", tieneAfluenciaSemana);
        metricas.put("tieneAfluenciaMes", tieneAfluenciaMes);

        // Distribución real de membresías activas por plan (sin porcentajes ficticios)
        metricas.put("distribucionPlanes", construirDistribucionPlanes(membresiasActivasLista));
        double tasaActivos = totalSocios > 0 ? Math.min(100.0, (membresiasActivas * 100.0) / totalSocios) : 0.0;
        metricas.put("tasaSociosActivos", Math.round(tasaActivos * 10.0) / 10.0);

        return metricas;
    }

    private List<Map<String, Object>> construirBarrasPorDia(List<RegistroAfluencia> registros,
                                                            LocalDate desde,
                                                            LocalDate hasta) {
        Map<DayOfWeek, Integer> acumulados = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek day : DayOfWeek.values()) {
            acumulados.put(day, 0);
        }

        for (RegistroAfluencia r : registros) {
            if (r.getFecha() != null
                    && !r.getFecha().isBefore(desde)
                    && !r.getFecha().isAfter(hasta)
                    && r.getConcurrenciaAprox() != null) {
                DayOfWeek dow = r.getFecha().getDayOfWeek();
                acumulados.put(dow, acumulados.get(dow) + Math.max(0, r.getConcurrenciaAprox()));
            }
        }

        int max = acumulados.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<Map<String, Object>> barras = new ArrayList<>();
        DayOfWeek[] orden = DayOfWeek.values();
        for (int i = 0; i < orden.length; i++) {
            int val = acumulados.get(orden[i]);
            int altura = (max > 0 && val > 0) ? Math.max(12, (int) Math.round((val * 100.0) / max)) : 0;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("dia", DIAS_CORTOS[i]);
            item.put("valor", val);
            item.put("altura", altura);
            barras.add(item);
        }
        return barras;
    }

    private String serializarBarrasJson(List<Map<String, Object>> barras) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < barras.size(); i++) {
            Map<String, Object> b = barras.get(i);
            if (i > 0) sb.append(',');
            sb.append("{\"dia\":\"").append(b.get("dia"))
              .append("\",\"valor\":").append(b.get("valor"))
              .append(",\"altura\":").append(b.get("altura"))
              .append('}');
        }
        sb.append(']');
        return sb.toString();
    }

    private List<Map<String, Object>> construirDistribucionPlanes(List<Membresia> activas) {
        Map<String, Long> conteo = new LinkedHashMap<>();
        Map<String, Double> precios = new HashMap<>();

        for (Membresia m : activas) {
            if (m.getPlan() != null && m.getPlan().getNombre() != null) {
                String nombre = m.getPlan().getNombre();
                conteo.put(nombre, conteo.getOrDefault(nombre, 0L) + 1L);
                if (m.getPlan().getPrecio() != null) {
                    precios.putIfAbsent(nombre, m.getPlan().getPrecio());
                }
            }
        }

        long total = activas.size();
        List<Map<String, Object>> resultado = new ArrayList<>();
        for (Map.Entry<String, Long> entry : conteo.entrySet()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("nombre", entry.getKey());
            row.put("precio", precios.getOrDefault(entry.getKey(), 0.0));
            row.put("cantidad", entry.getValue());
            int pct = total > 0 ? (int) Math.round((entry.getValue() * 100.0) / total) : 0;
            row.put("porcentaje", pct);
            resultado.add(row);
        }
        return resultado;
    }
}
