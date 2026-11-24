package pe.edu.utp.Grupo04.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pe.edu.utp.Grupo04.service.DashboardService;
import pe.edu.utp.Grupo04.service.ReservaService;

import java.util.Collections;
import java.util.Map;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;
    private final ReservaService reservaService;

    public DashboardController(DashboardService dashboardService,
                               ReservaService reservaService) {
        this.dashboardService = dashboardService;
        this.reservaService = reservaService;
    }

    @GetMapping({"/admin", "/admin/dashboard"})
    public String dashboard(Model model) {
        Map<String, Object> metricas = dashboardService.obtenerMetricasDashboard();
        model.addAttribute("metricas", metricas);
        model.addAttribute("proximasAVencer", metricas.getOrDefault("proximasAVencerLista", Collections.emptyList()));
        model.addAttribute("reservas", reservaService.listarTodas());
        return "admin/dashboard";
    }
}
