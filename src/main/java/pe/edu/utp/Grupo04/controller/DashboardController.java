package pe.edu.utp.Grupo04.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pe.edu.utp.Grupo04.service.DashboardService;
import pe.edu.utp.Grupo04.service.MembresiaService;
import pe.edu.utp.Grupo04.service.SocioService;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final SocioService socioService;
    private final MembresiaService membresiaService;

    public DashboardController(DashboardService dashboardService,
                               SocioService socioService,
                               MembresiaService membresiaService) {
        this.dashboardService = dashboardService;
        this.socioService = socioService;
        this.membresiaService = membresiaService;
    }

    @GetMapping({"/admin", "/admin/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("metricas", dashboardService.obtenerMetricasDashboard());
        model.addAttribute("socios", socioService.listarTodos());
        model.addAttribute("proximasAVencer", membresiaService.listarProximasAVencer(7));
        return "admin/dashboard";
    }
}
