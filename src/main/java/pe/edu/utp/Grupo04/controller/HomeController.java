package pe.edu.utp.Grupo04.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pe.edu.utp.Grupo04.service.PlanService;
import pe.edu.utp.Grupo04.service.ProductoService;
import pe.edu.utp.Grupo04.service.PromocionService;

@Controller
public class HomeController {

    private final PlanService planService;
    private final PromocionService promocionService;
    private final ProductoService productoService;

    public HomeController(PlanService planService,
                          PromocionService promocionService,
                          ProductoService productoService) {
        this.planService = planService;
        this.promocionService = promocionService;
        this.productoService = productoService;
    }

    @GetMapping({"/", "/inicio"})
    public String inicio(Model model) {
        model.addAttribute("planes", planService.listarActivos());
        model.addAttribute("promociones", promocionService.listarVigentes());
        model.addAttribute("productos", productoService.listarCatalogoActivo());
        return "public/inicio";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }
        return "redirect:/login?logout=true";
    }
}
