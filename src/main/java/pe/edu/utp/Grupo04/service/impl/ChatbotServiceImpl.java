package pe.edu.utp.Grupo04.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.utp.Grupo04.dto.ChatbotResponseDTO;
import pe.edu.utp.Grupo04.model.Membresia;
import pe.edu.utp.Grupo04.model.Plan;
import pe.edu.utp.Grupo04.model.Producto;
import pe.edu.utp.Grupo04.model.Promocion;
import pe.edu.utp.Grupo04.model.enums.BeneficioPlan;
import pe.edu.utp.Grupo04.model.enums.EstadoMembresia;
import pe.edu.utp.Grupo04.model.enums.FranjaHoraria;
import pe.edu.utp.Grupo04.model.enums.PeriodoMembresia;
import pe.edu.utp.Grupo04.service.AfluenciaService;
import pe.edu.utp.Grupo04.service.ChatbotService;
import pe.edu.utp.Grupo04.service.CongelamientoService;
import pe.edu.utp.Grupo04.service.GeminiChatClient;
import pe.edu.utp.Grupo04.service.MembresiaService;
import pe.edu.utp.Grupo04.service.PlanService;
import pe.edu.utp.Grupo04.service.ProductoService;
import pe.edu.utp.Grupo04.service.PromocionService;
import pe.edu.utp.Grupo04.service.TarifaInscripcion;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ChatbotServiceImpl implements ChatbotService {

    private static final String SYSTEM_PROMPT = """
            Eres el Coach IA oficial de Energy Gym (Sede Av. Las Américas, Ate — Lima).
            Reglas estrictas:
            1. Responde SOLO sobre Energy Gym, rutinas de entrenamiento, técnica de ejercicios, nutrición deportiva básica, suplementos de la tienda, afluencia, horarios y gestión de membresías.
            2. Si el usuario pregunta algo ajeno al gimnasio o fitness (tareas escolares, matemáticas, programación, política, chistes, redacción de ensayos, etc.), rechaza amablemente en una sola frase indicando que solo asistes en temas de Energy Gym y entrenamiento.
            3. Sé claro, motivador y conciso (máximo 120 palabras). Responde siempre en español y sin inventar precios ni promociones que no estén en el contexto.
            """;

    private static final List<String> PALABRAS_FUERA_DE_DOMINIO = List.of(
            "resuelve esta ecuacion", "ecuacion diferencial", "derivada de", "integral de",
            "haz mi tarea", "mi tarea de", "codigo en java", "codigo en python", "programa en",
            "html y css", "consulta sql", "ensayo sobre", "historia del peru", "guerra mundial",
            "traduce al ingles", "capital de francia", "presidentes del", "algoritmo de"
    );

    private static final List<String> PALABRAS_COACH_FITNESS = List.of(
            "rutina", "ejercicio", "musculo", "hipertrofia", "bajar de peso", "definicion",
            "volumen", "proteina", "creatina", "preentreno", "pre-entreno", "cardio",
            "pecho", "espalda", "pierna", "hombro", "biceps", "triceps", "abdomen",
            "dieta", "nutricion", "calorias", "entrenar", "series", "repeticiones", "recomiendame"
    );

    private final PlanService planService;
    private final PromocionService promocionService;
    private final ProductoService productoService;
    private final AfluenciaService afluenciaService;
    private final MembresiaService membresiaService;
    private final CongelamientoService congelamientoService;
    private final GeminiChatClient geminiChatClient;
    private final int limiteDiarioIa;
    private final Map<String, UsoDiario> contadorIaPorUsuario = new ConcurrentHashMap<>();

    public ChatbotServiceImpl(PlanService planService,
                              PromocionService promocionService,
                              ProductoService productoService,
                              AfluenciaService afluenciaService,
                              MembresiaService membresiaService,
                              CongelamientoService congelamientoService,
                              GeminiChatClient geminiChatClient,
                              @Value("${app.chatbot.ia-limite-diario:20}") int limiteDiarioIa) {
        this.planService = planService;
        this.promocionService = promocionService;
        this.productoService = productoService;
        this.afluenciaService = afluenciaService;
        this.membresiaService = membresiaService;
        this.congelamientoService = congelamientoService;
        this.geminiChatClient = geminiChatClient;
        this.limiteDiarioIa = Math.max(1, limiteDiarioIa);
    }

    @Override
    public ChatbotResponseDTO obtenerBienvenida(Authentication authentication) {
        ContextoUsuario ctx = resolverContextoUsuario(authentication);
        if (ctx.iaDisponible()) {
            String saludo = "¡Hola! Soy tu Coach IA de Energy Gym (membresía " + ctx.membresia().getPlan().getNombre()
                    + " activa). Puedo ayudarte con rutinas, ejercicios, suplementos, afluencia o tu membresía.";
            return new ChatbotResponseDTO(
                    true,
                    saludo,
                    "IA",
                    true,
                    consultasRestantes(ctx.username()),
                    List.of("Rutina de 4 días", "¿Cómo está la afluencia?", "Mi membresía", "Suplementos"),
                    List.of(Map.of("texto", "Mi membresía", "url", "/cliente/mi-membresia"))
            );
        }

        String saludo = "¡Hola! Soy el asistente virtual de Energy Gym. Pregúntame sobre planes, promociones, horarios, afluencia o suplementos. Con una membresía activa desbloqueas el Coach IA para rutinas y entrenamiento.";
        return new ChatbotResponseDTO(
                true,
                saludo,
                "LOCAL",
                false,
                null,
                List.of("Ver planes y precios", "Horarios y ubicación", "¿Cómo está la afluencia?", "Promociones vigentes"),
                List.of(Map.of("texto", "Ver planes", "url", "/planes"))
        );
    }

    @Override
    public ChatbotResponseDTO responder(String mensaje, Authentication authentication) {
        String limpio = mensaje == null ? "" : mensaje.trim();
        String normalizado = normalizar(limpio);
        ContextoUsuario ctx = resolverContextoUsuario(authentication);

        // 1. Pre-filtro anti-off-topic antes de gastar tokens
        if (esFueraDeDominio(normalizado)) {
            return new ChatbotResponseDTO(
                    true,
                    "Solo puedo ayudarte con temas relacionados con Energy Gym, membresías, horarios, suplementos y entrenamiento físico.",
                    "LOCAL",
                    ctx.iaDisponible(),
                    ctx.iaDisponible() ? consultasRestantes(ctx.username()) : null,
                    List.of("Ver planes y precios", "Horarios y ubicación", "¿Cómo está la afluencia?"),
                    List.of()
            );
        }

        // 2. Respuestas exactas desde BD (siempre gratis, rápidas y sin alucinaciones)
        ChatbotResponseDTO respuestaLocalDirecta = resolverConsultaDirecta(normalizado, ctx);
        if (respuestaLocalDirecta != null) {
            return respuestaLocalDirecta;
        }

        // 3. Consultas abiertas / entrenamiento / fitness:
        //    - Con membresía activa -> Coach IA (Gemini) con límite diario y fallback local
        //    - Sin membresía activa -> Invita a activar plan (costo S/ 0 en APIs)
        if (ctx.iaDisponible()) {
            int restantes = consultasRestantes(ctx.username());
            if (restantes <= 0) {
                return new ChatbotResponseDTO(
                        true,
                        "Alcanzaste tu límite diario de " + limiteDiarioIa + " consultas con el Coach IA. Mañana se renueva automáticamente. Mientras tanto puedes consultarme planes, horarios, afluencia o suplementos.",
                        "LOCAL",
                        true,
                        0,
                        List.of("¿Cómo está la afluencia?", "Suplementos", "Mi membresía"),
                        List.of(Map.of("texto", "Mi membresía", "url", "/cliente/mi-membresia"))
                );
            }

            Optional<String> respuestaIa = geminiChatClient.generarRespuesta(
                    SYSTEM_PROMPT, construirContextoParaIa(ctx), limpio);
            if (respuestaIa.isPresent()) {
                int nuevasRestantes = registrarUsoIa(ctx.username());
                return new ChatbotResponseDTO(
                        true,
                        respuestaIa.get(),
                        "IA",
                        true,
                        nuevasRestantes,
                        List.of("¿Cómo está la afluencia?", "Suplementos recomendados", "Mi membresía"),
                        List.of(Map.of("texto", "Ver mi rutina", "url", "/cliente/mi-membresia"))
                );
            }

            // Fallback limpio si no hay GEMINI_API_KEY o falla la red
            return new ChatbotResponseDTO(
                    true,
                    "El Coach IA está en modo sin conexión en este momento, pero puedo ayudarte al instante con tus datos de membresía, rutinas del portal, afluencia, planes y suplementos.",
                    "LOCAL_FALLBACK",
                    true,
                    restantes,
                    List.of("Mi membresía", "¿Cómo está la afluencia?", "Suplementos", "Horarios"),
                    List.of(Map.of("texto", "Ir a Mi Membresía", "url", "/cliente/mi-membresia"))
            );
        }

        if (contieneAlguna(normalizado, PALABRAS_COACH_FITNESS)) {
            return new ChatbotResponseDTO(
                    true,
                    "Las consultas personalizadas de rutinas, ejercicios y nutrición con nuestro Coach IA están incluidas al activar cualquiera de nuestros planes de membresía. ¡Elige tu plan y entrena con asesoría 24/7!",
                    "LOCAL",
                    false,
                    null,
                    List.of("Ver planes y precios", "Promociones vigentes", "Cómo inscribirme"),
                    List.of(
                            Map.of("texto", "Ver planes", "url", "/planes"),
                            Map.of("texto", "Inscribirme", "url", "/registrarse")
                    )
            );
        }

        return new ChatbotResponseDTO(
                true,
                "Puedo ayudarte con planes y tarifas, promociones vigentes, horarios y ubicación, afluencia del gimnasio, suplementos o cómo inscribirte. ¿Sobre qué tema deseas consultar?",
                "LOCAL",
                false,
                null,
                List.of("Ver planes y precios", "Horarios y ubicación", "¿Cómo está la afluencia?", "Suplementos"),
                List.of(Map.of("texto", "Ver planes", "url", "/planes"))
        );
    }

    private ChatbotResponseDTO resolverConsultaDirecta(String norm, ContextoUsuario ctx) {
        Integer rest = ctx.iaDisponible() ? consultasRestantes(ctx.username()) : null;

        if (esSaludo(norm)) {
            return obtenerBienvenida(ctx.authentication());
        }
        if (contieneAlguna(norm, List.of("mi membresia", "mi plan", "dias restantes", "cuando vence", "congelar", "congelamiento", "pausar"))) {
            if (!ctx.autenticado()) {
                return new ChatbotResponseDTO(
                        true,
                        "Para consultar tu membresía, días restantes o solicitar una pausa (hasta 15 días), inicia sesión en tu cuenta de socio.",
                        "LOCAL", false, null,
                        List.of("Ver planes y precios", "Cómo inscribirme"),
                        List.of(Map.of("texto", "Iniciar sesión", "url", "/login"))
                );
            }
            if (ctx.membresia() == null) {
                return new ChatbotResponseDTO(
                        true,
                        "Actualmente no tienes una membresía activa registrada. Puedes elegir un plan en línea o regularizar tu inscripción en recepción.",
                        "LOCAL", false, null,
                        List.of("Ver planes y precios", "Promociones vigentes"),
                        List.of(Map.of("texto", "Ver planes", "url", "/planes"))
                );
            }
            long dias = membresiaService.calcularDiasRestantes(ctx.membresia());
            String estadoTexto = ctx.congelada() ? "EN PAUSA" : "ACTIVA";
            return new ChatbotResponseDTO(
                    true,
                    "Tu plan actual es " + ctx.membresia().getPlan().getNombre() + " (" + estadoTexto
                            + "), vigente hasta el " + ctx.membresia().getFechaFin() + " (" + dias
                            + " días restantes). Puedes gestionar pausas y tu rutina desde Mi Membresía.",
                    "LOCAL", ctx.iaDisponible(), rest,
                    List.of("¿Cómo está la afluencia?", "Suplementos"),
                    List.of(Map.of("texto", "Ir a Mi Membresía", "url", "/cliente/mi-membresia"))
            );
        }
        if (contieneAlguna(norm, List.of("horario", "hora abren", "hora cierran", "ubicacion", "direccion", "donde queda", "telefono", "sede"))) {
            return new ChatbotResponseDTO(
                    true,
                    "Estamos en Av. Las Américas, Ate — Lima (Tel. (01) 480-9921 / +51 987 654 321). Horarios: Lun–Vie 06:00–23:00, Sábados 07:00–20:00 y Domingos/Feriados 08:00–14:00. Contamos con estacionamiento, lockers y duchas.",
                    "LOCAL", ctx.iaDisponible(), rest,
                    List.of("¿Cómo está la afluencia?", "Ver planes y precios"),
                    List.of(Map.of("texto", "Ver afluencia", "url", "/afluencia"))
            );
        }
        if (contieneAlguna(norm, List.of("afluencia", "lleno", "gente", "ocupacion", "capacidad", "franja"))) {
            Map<FranjaHoraria, Integer> mapa = afluenciaService.obtenerAfluenciaVigente();
            int m = mapa.getOrDefault(FranjaHoraria.MANANA, 0);
            int md = mapa.getOrDefault(FranjaHoraria.MEDIODIA, 0);
            int t = mapa.getOrDefault(FranjaHoraria.TARDE_NOCHE, 0);
            String texto = String.format(Locale.ROOT,
                    "Afluencia vigente (capacidad máx. %d personas): Mañana (06–11h): %d (%d%%) · Mediodía (11–16h): %d (%d%%) · Tarde-Noche (16–23h): %d (%d%%).",
                    AccesoServiceImpl.CAPACIDAD_MAXIMA, m, pct(m), md, pct(md), t, pct(t));
            return new ChatbotResponseDTO(
                    true, texto, "LOCAL", ctx.iaDisponible(), rest,
                    List.of("Horarios y ubicación", "Ver planes y precios"),
                    List.of(Map.of("texto", "Ver monitor de afluencia", "url", ctx.autenticado() ? "/cliente/afluencia" : "/afluencia"))
            );
        }
        if (contieneAlguna(norm, List.of("plan", "precio", "tarifa", "mensual", "trimestral", "anual", "cuanto cuesta", "beneficio"))) {
            List<Plan> planes = planService.listarActivos();
            if (planes.isEmpty()) {
                return new ChatbotResponseDTO(
                        true, "Por el momento no hay planes activos publicados. Consulta en recepción.",
                        "LOCAL", ctx.iaDisponible(), rest, List.of("Horarios y ubicación"), List.of());
            }
            String resumen = planes.stream().limit(4).map(p -> {
                String bens = p.getListaBeneficios().stream().limit(3).map(BeneficioPlan::getEtiqueta)
                        .collect(Collectors.joining(", "));
                return String.format(Locale.ROOT, "• %s: S/ %.2f/mes (Trimestral -10%%: S/ %.2f · Anual -20%%: S/ %.2f) — %s",
                        p.getNombre(), p.getPrecio(),
                        PeriodoMembresia.TRIMESTRAL.total(p.getPrecio()),
                        PeriodoMembresia.ANUAL.total(p.getPrecio()),
                        bens);
            }).collect(Collectors.joining("\n"));
            return new ChatbotResponseDTO(
                    true,
                    "Estos son nuestros planes activos:\n" + resumen,
                    "LOCAL", ctx.iaDisponible(), rest,
                    List.of("Promociones vigentes", "Cómo inscribirme", "Horarios y ubicación"),
                    List.of(Map.of("texto", "Comparar planes", "url", "/planes"))
            );
        }
        if (contieneAlguna(norm, List.of("promocion", "promo", "descuento", "cupon", "oferta", "2x1"))) {
            List<Promocion> promos = promocionService.listarVigentes();
            String texto;
            if (promos.isEmpty()) {
                texto = "Actualmente ofrecemos -10% en modalidad Trimestral y -20% en modalidad Anual en todos los planes.";
            } else {
                String lista = promos.stream().limit(3)
                        .map(p -> "• " + p.getTitulo() + (p.getDescripcion() != null ? ": " + p.getDescripcion() : ""))
                        .collect(Collectors.joining("\n"));
                texto = "Promociones vigentes:\n" + lista;
            }
            return new ChatbotResponseDTO(
                    true, texto, "LOCAL", ctx.iaDisponible(), rest,
                    List.of("Ver planes y precios", "Cómo inscribirme"),
                    List.of(Map.of("texto", "Ver promociones", "url", "/promociones"))
            );
        }
        if (contieneAlguna(norm, List.of("suplemento", "producto", "tienda", "catalogo", "comprar", "stock", "whey", "bcaa", "glutamina"))) {
            // Si es socio con IA y pregunta por recomendación personalizada (ej. "qué suplemento me recomiendas"), dejamos que pase a IA
            if (ctx.iaDisponible() && contieneAlguna(norm, List.of("recomiend", "cual me conviene", "para ganar", "para bajar", "tomar"))) {
                return null;
            }
            List<Producto> productos = productoService.listarCatalogoActivo();
            if (productos.isEmpty()) {
                return new ChatbotResponseDTO(
                        true, "No hay suplementos activos en catálogo en este momento.",
                        "LOCAL", ctx.iaDisponible(), rest, List.of("Ver planes y precios"), List.of());
            }
            String lista = productos.stream().limit(5)
                    .map(p -> String.format(Locale.ROOT, "• %s: S/ %.2f (%d en stock)",
                            p.getNombre(), p.getPrecio(), p.getStock() == null ? 0 : p.getStock()))
                    .collect(Collectors.joining("\n"));
            return new ChatbotResponseDTO(
                    true,
                    "Suplementos disponibles en tienda:\n" + lista,
                    "LOCAL", ctx.iaDisponible(), rest,
                    List.of("Ver planes y precios", "Horarios y ubicación"),
                    List.of(Map.of("texto", "Ir a tienda", "url", ctx.autenticado() ? "/cliente/productos" : "/productos"))
            );
        }
        if (contieneAlguna(norm, List.of("inscribir", "registro", "registrarme", "pagar", "pago", "carnet", "qr", "acceso"))) {
            return new ChatbotResponseDTO(
                    true,
                    "Puedes inscribirte 100% en línea eligiendo tu plan y modalidad (Mensual, Trimestral o Anual). Puedes confirmar con Pago Simulado (activación inmediata) o Pago en Recepción (congela tu tarifa por 72 horas). Al activarte recibes tu Carnet Digital con QR para ingresar por el torniquete.",
                    "LOCAL", ctx.iaDisponible(), rest,
                    List.of("Ver planes y precios", "Promociones vigentes"),
                    List.of(Map.of("texto", "Registrarme ahora", "url", "/registrarse"))
            );
        }
        return null;
    }

    private String construirContextoParaIa(ContextoUsuario ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("Sede: Av. Las Américas, Ate — Lima. Horario: Lun–Vie 06:00–23:00, Sáb 07:00–20:00, Dom 08:00–14:00.\n");
        if (ctx.membresia() != null && ctx.membresia().getPlan() != null) {
            long dias = membresiaService.calcularDiasRestantes(ctx.membresia());
            sb.append("Socio autenticado con plan activo: ").append(ctx.membresia().getPlan().getNombre())
                    .append(" (").append(dias).append(" días restantes).\n");
        }
        Map<FranjaHoraria, Integer> afluencia = afluenciaService.obtenerAfluenciaVigente();
        sb.append("Afluencia hoy (máx. 80): Mañana=").append(afluencia.getOrDefault(FranjaHoraria.MANANA, 0))
                .append(", Mediodía=").append(afluencia.getOrDefault(FranjaHoraria.MEDIODIA, 0))
                .append(", Tarde-Noche=").append(afluencia.getOrDefault(FranjaHoraria.TARDE_NOCHE, 0)).append(".\n");

        List<Producto> prods = productoService.listarCatalogoActivo();
        if (!prods.isEmpty()) {
            sb.append("Suplementos en stock: ")
                    .append(prods.stream().limit(6)
                            .map(p -> p.getNombre() + " (S/ " + p.getPrecio() + ")")
                            .collect(Collectors.joining(", ")))
                    .append(".\n");
        }
        return sb.toString();
    }

    private ContextoUsuario resolverContextoUsuario(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return new ContextoUsuario(null, null, false, null, false, false);
        }
        boolean esCliente = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_CLIENTE".equals(a.getAuthority()));
        if (!esCliente) {
            return new ContextoUsuario(auth, auth.getName(), true, null, false, false);
        }
        LocalDate hoy = LocalDate.now(TarifaInscripcion.ZONA_LIMA);
        Membresia membresia = membresiaService.obtenerMembresiaActivaPorUsername(auth.getName())
                .filter(m -> m.getEstado() == EstadoMembresia.ACTIVA
                        && m.getFechaInicio() != null && !m.getFechaInicio().isAfter(hoy)
                        && m.getFechaFin() != null && !m.getFechaFin().isBefore(hoy))
                .orElse(null);
        boolean congelada = membresia != null && congelamientoService.membresiaCongelada(membresia.getId(), hoy);
        boolean iaDisponible = membresia != null && !congelada;
        return new ContextoUsuario(auth, auth.getName(), true, membresia, congelada, iaDisponible);
    }

    private int consultasRestantes(String username) {
        if (username == null) return 0;
        LocalDate hoy = LocalDate.now(TarifaInscripcion.ZONA_LIMA);
        UsoDiario uso = contadorIaPorUsuario.get(username);
        if (uso == null || !hoy.equals(uso.fecha())) {
            return limiteDiarioIa;
        }
        return Math.max(0, limiteDiarioIa - uso.usadas());
    }

    private int registrarUsoIa(String username) {
        LocalDate hoy = LocalDate.now(TarifaInscripcion.ZONA_LIMA);
        UsoDiario actualizado = contadorIaPorUsuario.compute(username, (k, prev) -> {
            if (prev == null || !hoy.equals(prev.fecha())) {
                return new UsoDiario(hoy, 1);
            }
            return new UsoDiario(hoy, prev.usadas() + 1);
        });
        return Math.max(0, limiteDiarioIa - actualizado.usadas());
    }

    private static boolean esFueraDeDominio(String norm) {
        return contieneAlguna(norm, PALABRAS_FUERA_DE_DOMINIO);
    }

    private static boolean esSaludo(String norm) {
        return norm.equals("hola") || norm.equals("buenas") || norm.equals("buenos dias")
                || norm.equals("buenas tardes") || norm.equals("buenas noches")
                || norm.equals("ayuda") || norm.equals("menu");
    }

    private static boolean contieneAlguna(String texto, List<String> palabras) {
        for (String p : palabras) {
            if (texto.contains(p)) {
                return true;
            }
        }
        return false;
    }

    private static String normalizar(String texto) {
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinTildes.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }

    private static int pct(int personas) {
        if (personas <= 0) return 0;
        return Math.min(100, (int) Math.round((personas * 100.0) / AccesoServiceImpl.CAPACIDAD_MAXIMA));
    }

    private record ContextoUsuario(
            Authentication authentication,
            String username,
            boolean autenticado,
            Membresia membresia,
            boolean congelada,
            boolean iaDisponible
    ) {}

    private record UsoDiario(LocalDate fecha, int usadas) {}
}
