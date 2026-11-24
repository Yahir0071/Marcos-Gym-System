package pe.edu.utp.Grupo04.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import pe.edu.utp.Grupo04.model.Membresia;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.service.MembresiaService;
import pe.edu.utp.Grupo04.service.SocioService;

import java.util.Optional;

@ControllerAdvice
public class GlobalModelAdvice {

    private final SocioService socioService;
    private final MembresiaService membresiaService;

    public GlobalModelAdvice(SocioService socioService, MembresiaService membresiaService) {
        this.socioService = socioService;
        this.membresiaService = membresiaService;
    }

    /**
     * M13: Consolida en un único @ModelAttribute la carga de socio, membresía y días restantes.
     * - Omite consultas en rutas administrativas, técnicas, APIs JSON y vistas públicas que no los consumen.
     * - Consulta la membresía activa una sola vez (antes se consultaba dos veces por petición).
     */
    @ModelAttribute
    public void populateClientContext(Model model, Authentication authentication, HttpServletRequest request) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return;
        }

        String uri = request != null ? request.getRequestURI() : "";
        if (uri == null || uri.startsWith("/api/") || uri.startsWith("/cliente/api/")
                || uri.startsWith("/admin") || uri.startsWith("/tecnico")) {
            return;
        }

        boolean esVistaCliente = uri.startsWith("/cliente/");
        boolean esDetallePublico = "/detalle-producto".equals(uri);
        if (!esVistaCliente && !esDetallePublico) {
            return;
        }

        String username = authentication.getName();
        Socio socio = socioService.buscarPorUsername(username).orElse(null);
        model.addAttribute("socio", socio);

        if (esVistaCliente) {
            Optional<Membresia> membresiaOpt = membresiaService.obtenerMembresiaActivaPorUsername(username);
            Membresia membresia = membresiaOpt.orElse(null);
            long diasRestantes = membresiaOpt.map(membresiaService::calcularDiasRestantes).orElse(0L);
            model.addAttribute("membresia", membresia);
            model.addAttribute("diasRestantes", diasRestantes);
        }
    }
}
