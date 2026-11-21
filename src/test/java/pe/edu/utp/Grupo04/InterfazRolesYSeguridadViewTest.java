package pe.edu.utp.Grupo04;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import pe.edu.utp.Grupo04.model.Membresia;
import pe.edu.utp.Grupo04.model.Plan;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.model.Usuario;
import pe.edu.utp.Grupo04.model.enums.EstadoMembresia;
import pe.edu.utp.Grupo04.model.enums.Rol;
import pe.edu.utp.Grupo04.repository.MembresiaRepository;
import pe.edu.utp.Grupo04.repository.PagoRepository;
import pe.edu.utp.Grupo04.repository.PlanRepository;
import pe.edu.utp.Grupo04.repository.RegistroAfluenciaRepository;
import pe.edu.utp.Grupo04.repository.SocioRepository;
import pe.edu.utp.Grupo04.repository.SolicitudCongelamientoRepository;
import pe.edu.utp.Grupo04.repository.UsuarioRepository;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:joseph_ui_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
public class InterfazRolesYSeguridadViewTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SocioRepository socioRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PlanRepository planRepository;
    @Autowired
    private MembresiaRepository membresiaRepository;
    @Autowired
    private PagoRepository pagoRepository;
    @Autowired
    private RegistroAfluenciaRepository afluenciaRepository;
    @Autowired
    private SolicitudCongelamientoRepository congelamientoRepository;

    @BeforeEach
    void cleanDb() {
        congelamientoRepository.deleteAll();
        pagoRepository.deleteAll();
        membresiaRepository.deleteAll();
        socioRepository.deleteAll();
        usuarioRepository.deleteAll();
        planRepository.deleteAll();
        afluenciaRepository.deleteAll();
    }

    private MockHttpSession sessionWithRole(String username, String role) {
        if (usuarioRepository.findByUsername(username).isEmpty()) {
            Usuario usuario = new Usuario();
            usuario.setUsername(username);
            usuario.setPassword("fixture");
            usuario.setRol(Rol.valueOf(role));
            usuarioRepository.save(usuario);
        }
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                username,
                "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        SecurityContext context = new SecurityContextImpl(auth);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return session;
    }

    @Test
    @DisplayName("D7 - Navbar público muestra accesos distintos por cada uno de los 4 roles sin menú de acceso total")
    void testNavbarCuatroRolesDiferenciados() throws Exception {
        // Cliente
        mockMvc.perform(get("/inicio").session(sessionWithRole("cli@energygym.pe", "CLIENTE")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("MI PORTAL")))
                .andExpect(content().string(not(containsString("PANEL ADMIN"))))
                .andExpect(content().string(not(containsString("GESTOR TÉCNICO"))));

        // Recepción
        mockMvc.perform(get("/inicio").session(sessionWithRole("rec@energygym.pe", "RECEPCION")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("RECEPCIÓN")))
                .andExpect(content().string(not(containsString("PANEL ADMIN"))))
                .andExpect(content().string(not(containsString("MI PORTAL"))))
                .andExpect(content().string(not(containsString("GESTOR TÉCNICO"))));

        // Administración
        mockMvc.perform(get("/inicio").session(sessionWithRole("adm@energygym.pe", "ADMINISTRACION")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("PANEL ADMIN")))
                .andExpect(content().string(not(containsString("MI PORTAL"))))
                .andExpect(content().string(not(containsString("GESTOR TÉCNICO"))));

        // Gestor Técnico
        mockMvc.perform(get("/inicio").session(sessionWithRole("tec@energygym.pe", "GESTOR_TECNICO")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("GESTOR TÉCNICO")))
                .andExpect(content().string(containsString("/tecnico/cuentas")))
                .andExpect(content().string(not(containsString("PANEL ADMIN"))))
                .andExpect(content().string(not(containsString("MI PORTAL"))));
    }

    @Test
    @DisplayName("D4 / M07 - Dashboard sin pagos muestra 0.00 y nunca 16,450.00 prefijado; sin enlace VISTA CLIENTE")
    void testDashboardSinPagosMuestraCeroYSinMenuUniversal() throws Exception {
        mockMvc.perform(get("/admin/dashboard").session(sessionWithRole("admin@energygym.pe", "ADMINISTRACION")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("0.00")))
                .andExpect(content().string(containsString("SIN PAGOS")))
                .andExpect(content().string(not(containsString("16,450"))))
                .andExpect(content().string(not(containsString("VISTA CLIENTE"))));
    }

    @Test
    @DisplayName("D1 / C04 + D3 - Nombre de socio con payload HTML se renderiza escapado como texto y usa btn-edit-socio")
    void testSociosEscapaPayloadHtmlYUsaContratoRest() throws Exception {
        Usuario u = new Usuario();
        u.setUsername("xss@energygym.pe");
        u.setPassword("encoded");
        u.setRol(Rol.CLIENTE);
        u.setActivo(true);

        Socio s = new Socio();
        s.setDni("70123456");
        s.setNombres("<img src=x onerror=alert(1)>");
        s.setApellidos("Prueba XSS");
        s.setTelefono("987654321");
        s.setUsuario(u);
        socioRepository.save(s);

        mockMvc.perform(get("/admin/socios").session(sessionWithRole("rec@energygym.pe", "RECEPCION")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("&lt;img src=x onerror=alert(1)&gt;")))
                .andExpect(content().string(not(containsString("<img src=x onerror=alert(1)>"))))
                .andExpect(content().string(containsString("btn-edit-socio")))
                .andExpect(content().string(not(containsString("btn-edit-row"))));
    }

    @Test
    @DisplayName("D5 / M08 + D6 - Enlaces sociales reales, codificación UTF-8 limpia y afluencia con estado real sin registros")
    void testPublicVistasUtf8EnlacesRealesYAfluenciaReal() throws Exception {
        mockMvc.perform(get("/afluencia"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("SIN REGISTROS")))
                .andExpect(content().string(not(containsString("https://wa.me/51987654321"))))
                .andExpect(content().string(not(containsString("javascript:void(0)"))))
                .andExpect(content().string(not(containsString("\ufffd"))));

        mockMvc.perform(get("/planes"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Las Américas")))
                .andExpect(content().string(containsString("prototipo académico")))
                .andExpect(content().string(not(containsString("\ufffd"))));
    }

    @Test
    @DisplayName("D5 / M08 - Endpoint /cliente/api/congelamiento persiste y lista solicitudes del socio autenticado")
    void testCongelamientoApiPersistente() throws Exception {
        Usuario u = new Usuario();
        u.setUsername("socio.freeze@energygym.pe");
        u.setPassword("encoded");
        u.setRol(Rol.CLIENTE);
        u.setActivo(true);

        Socio s = new Socio();
        s.setDni("71234567");
        s.setNombres("Lucia");
        s.setApellidos("Ramos");
        s.setUsuario(u);
        socioRepository.save(s);

        Plan plan = new Plan();
        plan.setNombre("Plan Plus");
        plan.setPrecio(119.0);
        plan.setDuracionDias(30);
        plan.setActivo(true);
        plan = planRepository.save(plan);

        Membresia m = new Membresia();
        m.setSocio(s);
        m.setPlan(plan);
        m.setFechaInicio(LocalDate.now().minusDays(2));
        m.setFechaFin(LocalDate.now().plusDays(28));
        m.setEstado(EstadoMembresia.ACTIVA);
        membresiaRepository.save(m);

        MockHttpSession clienteSession = sessionWithRole("socio.freeze@energygym.pe", "CLIENTE");
        String payload = String.format(
                "{\"dias\":7,\"fechaInicioSolicitada\":\"%s\",\"motivo\":\"Viaje de estudios programado\"}",
                LocalDate.now().plusDays(1)
        );

        mockMvc.perform(post("/cliente/api/congelamiento")
                        .session(clienteSession)
                        .with(csrf())
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.solicitud.dias").value(7))
                .andExpect(jsonPath("$.solicitud.estado").value("PENDIENTE"));

        mockMvc.perform(get("/cliente/api/congelamiento").session(clienteSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solicitudes.length()").value(1))
                .andExpect(jsonPath("$.solicitudes[0].motivo").value("Viaje de estudios programado"));
    }
}
