package pe.edu.utp.Grupo04.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import pe.edu.utp.Grupo04.model.Membresia;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.service.MembresiaService;
import pe.edu.utp.Grupo04.service.SocioService;

@ControllerAdvice
public class GlobalModelAdvice {

    private final SocioService socioService;
    private final MembresiaService membresiaService;

    public GlobalModelAdvice(SocioService socioService, MembresiaService membresiaService) {
        this.socioService = socioService;
        this.membresiaService = membresiaService;
    }

    @ModelAttribute("socio")
    public Socio currentSocio(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            return socioService.buscarPorUsername(authentication.getName()).orElse(null);
        }
        return null;
    }

    @ModelAttribute("membresia")
    public Membresia currentMembresia(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            return membresiaService.obtenerMembresiaActivaPorUsername(authentication.getName()).orElse(null);
        }
        return null;
    }
}
