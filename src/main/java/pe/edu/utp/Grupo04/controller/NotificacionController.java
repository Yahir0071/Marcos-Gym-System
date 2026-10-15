package pe.edu.utp.Grupo04.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import pe.edu.utp.Grupo04.model.Membresia;
import pe.edu.utp.Grupo04.service.MembresiaService;
import pe.edu.utp.Grupo04.service.PromocionService;
import pe.edu.utp.Grupo04.service.ReservaService;
import pe.edu.utp.Grupo04.service.SocioService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class NotificacionController {

    private final SocioService socioService;
    private final MembresiaService membresiaService;
    private final ReservaService reservaService;
    private final PromocionService promocionService;

    public NotificacionController(SocioService socioService,
                                  MembresiaService membresiaService,
                                  ReservaService reservaService,
                                  PromocionService promocionService) {
        this.socioService = socioService;
        this.membresiaService = membresiaService;
        this.reservaService = reservaService;
        this.promocionService = promocionService;
    }

    @GetMapping("/cliente/notificaciones")
    public String notificacionesCliente(Authentication authentication, Model model) {
        String username = authentication.getName();
        List<Map<String, String>> notificaciones = new ArrayList<>();

        // 1. Alerta si la membresía está por vencer
        Optional<Membresia> membresiaOpt = membresiaService.obtenerMembresiaActivaPorUsername(username);
        if (membresiaOpt.isPresent()) {
            Membresia m = membresiaOpt.get();
            long dias = membresiaService.calcularDiasRestantes(m);
            if (dias <= 7) {
                Map<String, String> notif = new HashMap<>();
                notif.put("tipo", "alerta");
                notif.put("titulo", "Membresía próxima a vencer");
                notif.put("mensaje", "Te quedan " + dias + " días de membresía (" + m.getPlan().getNombre() + "). ¡Renueva pronto!");
                notificaciones.add(notif);
            }
        }

        // 2. Avisos de estado de reservas
        reservaService.listarPorSocioUsername(username).forEach(r -> {
            Map<String, String> notif = new HashMap<>();
            notif.put("tipo", "info");
            notif.put("titulo", "Reserva de " + r.getProducto().getNombre());
            notif.put("mensaje", "Estado actual: " + r.getEstado().name() + " (" + r.getCantidad() + " unidad(es)).");
            notificaciones.add(notif);
        });

        // 3. Avisos de promociones vigentes
        promocionService.listarVigentes().forEach(p -> {
            Map<String, String> notif = new HashMap<>();
            notif.put("tipo", "promocion");
            notif.put("titulo", "¡Nueva Promoción: " + p.getTitulo() + "!");
            notif.put("mensaje", p.getDescripcion() != null ? p.getDescripcion() : "Aprovecha los descuentos vigentes.");
            notificaciones.add(notif);
        });

        model.addAttribute("notificaciones", notificaciones);
        socioService.buscarPorUsername(username).ifPresent(s -> model.addAttribute("socio", s));

        return "cliente/notificaciones";
    }
}
