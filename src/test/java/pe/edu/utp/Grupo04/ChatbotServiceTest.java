package pe.edu.utp.Grupo04;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import pe.edu.utp.Grupo04.dto.ChatbotResponseDTO;
import pe.edu.utp.Grupo04.model.Membresia;
import pe.edu.utp.Grupo04.model.Plan;
import pe.edu.utp.Grupo04.model.enums.EstadoMembresia;
import pe.edu.utp.Grupo04.service.AfluenciaService;
import pe.edu.utp.Grupo04.service.CongelamientoService;
import pe.edu.utp.Grupo04.service.GeminiChatClient;
import pe.edu.utp.Grupo04.service.MembresiaService;
import pe.edu.utp.Grupo04.service.PlanService;
import pe.edu.utp.Grupo04.service.ProductoService;
import pe.edu.utp.Grupo04.service.PromocionService;
import pe.edu.utp.Grupo04.service.TarifaInscripcion;
import pe.edu.utp.Grupo04.service.impl.ChatbotServiceImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    @Mock
    private PlanService planService;
    @Mock
    private PromocionService promocionService;
    @Mock
    private ProductoService productoService;
    @Mock
    private AfluenciaService afluenciaService;
    @Mock
    private MembresiaService membresiaService;
    @Mock
    private CongelamientoService congelamientoService;
    @Mock
    private GeminiChatClient geminiChatClient;

    private ChatbotServiceImpl chatbotService;
    private Authentication authCliente;
    private Membresia membresiaActiva;

    @BeforeEach
    void setUp() {
        chatbotService = new ChatbotServiceImpl(
                planService,
                promocionService,
                productoService,
                afluenciaService,
                membresiaService,
                congelamientoService,
                geminiChatClient,
                2
        );

        authCliente = new UsernamePasswordAuthenticationToken(
                "socio@energygym.pe",
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))
        );

        Plan planPlus = new Plan();
        planPlus.setId(2L);
        planPlus.setNombre("Plan Plus");
        planPlus.setPrecio(119.0);
        planPlus.setDuracionDias(30);

        LocalDate hoy = LocalDate.now(TarifaInscripcion.ZONA_LIMA);
        membresiaActiva = new Membresia();
        membresiaActiva.setId(10L);
        membresiaActiva.setPlan(planPlus);
        membresiaActiva.setEstado(EstadoMembresia.ACTIVA);
        membresiaActiva.setFechaInicio(hoy.minusDays(5));
        membresiaActiva.setFechaFin(hoy.plusDays(25));
    }

    @Test
    @DisplayName("Visitante anónimo consulta planes usando datos reales de BD sin llamar a IA")
    void visitanteConsultaPlanes_ModoLocal() {
        Plan plan = new Plan();
        plan.setNombre("Plan Básico");
        plan.setPrecio(79.0);
        plan.setDuracionDias(30);
        when(planService.listarActivos()).thenReturn(List.of(plan));

        ChatbotResponseDTO res = chatbotService.responder("¿Qué planes y precios tienen?", null);

        assertTrue(res.success());
        assertEquals("LOCAL", res.modo());
        assertFalse(res.iaDisponible());
        assertTrue(res.respuesta().contains("Plan Básico"));
        verify(geminiChatClient, never()).generarRespuesta(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Visitante sin membresía que pide rutina recibe invitación a activar plan sin gastar tokens IA")
    void visitantePideRutina_NoLlamaIA() {
        ChatbotResponseDTO res = chatbotService.responder("Recomiéndame una rutina de hipertrofia de 4 días", null);

        assertTrue(res.success());
        assertEquals("LOCAL", res.modo());
        assertFalse(res.iaDisponible());
        assertTrue(res.respuesta().contains("Coach IA"));
        verify(geminiChatClient, never()).generarRespuesta(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Bloquea consultas fuera de dominio (tareas/código) antes de llamar a Gemini")
    void consultaFueraDeDominio_BloqueadaAntesDeIA() {
        when(membresiaService.obtenerMembresiaActivaPorUsername("socio@energygym.pe"))
                .thenReturn(Optional.of(membresiaActiva));
        when(congelamientoService.membresiaCongelada(10L, LocalDate.now(TarifaInscripcion.ZONA_LIMA)))
                .thenReturn(false);

        ChatbotResponseDTO res = chatbotService.responder("Haz mi tarea de programación y dame código en Java", authCliente);

        assertTrue(res.success());
        assertEquals("LOCAL", res.modo());
        assertTrue(res.respuesta().contains("Solo puedo ayudarte con temas relacionados con Energy Gym"));
        verify(geminiChatClient, never()).generarRespuesta(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Socio con membresía activa accede al Coach IA y respeta el límite diario")
    void socioConPlanActivo_UsaCoachIAYRespetaLimite() {
        LocalDate hoy = LocalDate.now(TarifaInscripcion.ZONA_LIMA);
        when(membresiaService.obtenerMembresiaActivaPorUsername("socio@energygym.pe"))
                .thenReturn(Optional.of(membresiaActiva));
        when(congelamientoService.membresiaCongelada(10L, hoy)).thenReturn(false);
        when(membresiaService.calcularDiasRestantes(membresiaActiva)).thenReturn(25L);
        when(afluenciaService.obtenerAfluenciaVigente()).thenReturn(Map.of());
        when(productoService.listarCatalogoActivo()).thenReturn(List.of());
        when(geminiChatClient.generarRespuesta(anyString(), anyString(), anyString()))
                .thenReturn(Optional.of("Te recomiendo dividir en torso/pierna 4 días a la semana."));

        ChatbotResponseDTO r1 = chatbotService.responder("Armame una rutina de 4 días para ganar masa", authCliente);
        ChatbotResponseDTO r2 = chatbotService.responder("¿Cómo mejorar mi técnica de sentadilla?", authCliente);
        ChatbotResponseDTO r3 = chatbotService.responder("¿Cuántas series de bíceps hacer?", authCliente);

        assertEquals("IA", r1.modo());
        assertEquals(1, r1.consultasRestantesIa());
        assertEquals("IA", r2.modo());
        assertEquals(0, r2.consultasRestantesIa());
        assertEquals("LOCAL", r3.modo());
        assertTrue(r3.respuesta().contains("límite diario"));
    }

    @Test
    @DisplayName("Si Gemini no está configurado o falla, cae al fallback local sin error")
    void socioConPlanActivo_FallbackCuandoGeminiFalla() {
        LocalDate hoy = LocalDate.now(TarifaInscripcion.ZONA_LIMA);
        when(membresiaService.obtenerMembresiaActivaPorUsername("socio@energygym.pe"))
                .thenReturn(Optional.of(membresiaActiva));
        when(congelamientoService.membresiaCongelada(10L, hoy)).thenReturn(false);
        when(membresiaService.calcularDiasRestantes(membresiaActiva)).thenReturn(25L);
        when(afluenciaService.obtenerAfluenciaVigente()).thenReturn(Map.of());
        when(productoService.listarCatalogoActivo()).thenReturn(List.of());
        when(geminiChatClient.generarRespuesta(anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        ChatbotResponseDTO res = chatbotService.responder("¿Qué rutina me recomiendas para pecho?", authCliente);

        assertTrue(res.success());
        assertEquals("LOCAL_FALLBACK", res.modo());
        assertTrue(res.iaDisponible());
    }
}
