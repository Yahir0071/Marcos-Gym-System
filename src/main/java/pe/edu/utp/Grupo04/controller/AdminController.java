package pe.edu.utp.Grupo04.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminController {

    @GetMapping({"/admin", "/admin/dashboard"})
    public String dashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/admin/afluencia")
    public String afluencia() {
        return "admin/afluencia";
    }

    @GetMapping("/admin/equipos")
    public String equipos() {
        return "admin/equipos";
    }

    @GetMapping("/admin/mantenimientos")
    public String mantenimientos() {
        return "admin/mantenimientos";
    }

    @GetMapping("/admin/membresias")
    public String membresias() {
        return "admin/membresias";
    }

    @GetMapping("/admin/pagos")
    public String pagos() {
        return "admin/pagos";
    }

    @GetMapping("/admin/planes")
    public String planes() {
        return "admin/planes";
    }

    @GetMapping("/admin/productos")
    public String productos() {
        return "admin/productos";
    }

    @GetMapping("/admin/promociones")
    public String promociones() {
        return "admin/promociones";
    }

    @GetMapping("/admin/reporte-mantenimiento")
    public String reporteMantenimiento() {
        return "admin/reporte-mantenimiento";
    }

    @GetMapping("/admin/reservas")
    public String reservas() {
        return "admin/reservas";
    }

    @GetMapping("/admin/socios")
    public String socios() {
        return "admin/socios";
    }
}