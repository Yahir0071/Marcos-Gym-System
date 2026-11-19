package pe.edu.utp.Grupo04;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import pe.edu.utp.Grupo04.controller.GlobalModelAdvice;
import pe.edu.utp.Grupo04.model.Membresia;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.service.MembresiaService;
import pe.edu.utp.Grupo04.service.SocioService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GlobalModelAdviceTest {

    @Mock
    private SocioService socioService;
    @Mock
    private MembresiaService membresiaService;

    private GlobalModelAdvice advice;
    private Authentication clienteAuth;

    @BeforeEach
    void setUp() {
        advice = new GlobalModelAdvice(socioService, membresiaService);
        clienteAuth = new UsernamePasswordAuthenticationToken(
                "socio@energygym.pe",
                "secret",
                List.of(new SimpleGrantedAuthority("ROLE_CLIENTE"))
        );
    }

    @Test
    @DisplayName("M13 - En rutas /admin/**, /tecnico/**, /api/** y /inicio realiza 0 consultas repetidas")
    void testOmitirConsultasEnRutasQueNoConsumenContextoCliente() {
        String[] rutasOmitidas = {
                "/admin/dashboard",
                "/admin/socios",
                "/tecnico/cuentas",
                "/api/admin/socios/1",
                "/cliente/api/rutina",
                "/inicio",
                "/planes"
        };

        for (String uri : rutasOmitidas) {
            Model model = new ExtendedModelMap();
            MockHttpServletRequest req = new MockHttpServletRequest("GET", uri);
            advice.populateClientContext(model, clienteAuth, req);
            assertFalse(model.containsAttribute("socio"), "No debe poblar socio en " + uri);
        }

        verifyNoInteractions(socioService);
        verifyNoInteractions(membresiaService);
    }

    @Test
    @DisplayName("M13 - En /cliente/mi-membresia consulta socio 1 vez y membresía 1 sola vez (elimina consulta duplicada)")
    void testConsultaUnicaEnVistasCliente() {
        Socio socio = new Socio();
        socio.setId(5L);
        socio.setNombres("Ana");
        socio.setApellidos("Torres");

        Membresia membresia = new Membresia();
        membresia.setId(12L);

        when(socioService.buscarPorUsername("socio@energygym.pe")).thenReturn(Optional.of(socio));
        when(membresiaService.obtenerMembresiaActivaPorUsername("socio@energygym.pe")).thenReturn(Optional.of(membresia));
        when(membresiaService.calcularDiasRestantes(membresia)).thenReturn(18L);

        Model model = new ExtendedModelMap();
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/cliente/mi-membresia");

        advice.populateClientContext(model, clienteAuth, req);

        assertSame(socio, model.getAttribute("socio"));
        assertSame(membresia, model.getAttribute("membresia"));
        assertEquals(18L, model.getAttribute("diasRestantes"));

        // Antes: 1 llamada a socioService + 2 llamadas a membresiaService.obtenerMembresiaActivaPorUsername = 3 consultas.
        // Ahora: 1 llamada a socioService + 1 llamada a membresiaService = 2 consultas.
        verify(socioService, times(1)).buscarPorUsername("socio@energygym.pe");
        verify(membresiaService, times(1)).obtenerMembresiaActivaPorUsername("socio@energygym.pe");
    }

    @Test
    @DisplayName("M13 - En /detalle-producto consulta únicamente socio (0 consultas de membresía)")
    void testConsultaReducidaEnDetallePublico() {
        Socio socio = new Socio();
        socio.setId(7L);
        when(socioService.buscarPorUsername("socio@energygym.pe")).thenReturn(Optional.of(socio));

        Model model = new ExtendedModelMap();
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/detalle-producto");

        advice.populateClientContext(model, clienteAuth, req);

        assertSame(socio, model.getAttribute("socio"));
        verify(socioService, times(1)).buscarPorUsername("socio@energygym.pe");
        verifyNoInteractions(membresiaService);
    }
}
