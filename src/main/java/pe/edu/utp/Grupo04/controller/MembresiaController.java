package pe.edu.utp.Grupo04.controller;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.utp.Grupo04.dto.MembresiaAsignacionDTO;
import pe.edu.utp.Grupo04.model.Membresia;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.service.MembresiaService;
import pe.edu.utp.Grupo04.service.PlanService;
import pe.edu.utp.Grupo04.service.SocioService;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;

@Controller
public class MembresiaController {

    private final MembresiaService membresiaService;
    private final SocioService socioService;
    private final PlanService planService;

    public MembresiaController(MembresiaService membresiaService,
                               SocioService socioService,
                               PlanService planService) {
        this.membresiaService = membresiaService;
        this.socioService = socioService;
        this.planService = planService;
    }

    // --- CONSULTA DE MEMBRESÍA PARA CLIENTE ---
    @GetMapping("/cliente/mi-membresia")
    public String miMembresiaCliente(Authentication authentication, Model model) {
        String username = authentication.getName();
        Optional<Socio> socioOpt = socioService.buscarPorUsername(username);
        Optional<Membresia> membresiaOpt = membresiaService.obtenerMembresiaActivaPorUsername(username);

        socioOpt.ifPresent(socio -> model.addAttribute("socio", socio));

        if (membresiaOpt.isPresent()) {
            Membresia membresia = membresiaOpt.get();
            long diasRestantes = membresiaService.calcularDiasRestantes(membresia);
            boolean porVencer = membresiaService.estaPorVencer(membresia, 7);

            model.addAttribute("membresia", membresia);
            model.addAttribute("diasRestantes", diasRestantes);
            model.addAttribute("estaPorVencer", porVencer);
        } else {
            model.addAttribute("membresia", null);
            model.addAttribute("diasRestantes", 0);
            model.addAttribute("estaPorVencer", false);
        }

        return "cliente/mi-membresia";
    }

    // --- VALIDACIÓN DE TORNIQUETE Y CONTROL DE ACCESO EN VIVO (RF16/RF17) ---
    @PostMapping("/cliente/api/torniquete/validar")
    @ResponseBody
    public ResponseEntity<?> validarAccesoTorniquete(@RequestParam(required = false) String token,
                                                     @RequestParam(required = false) String dni,
                                                     Authentication authentication) {
        String username = (authentication != null && authentication.isAuthenticated()) ? authentication.getName() : null;
        Socio socio = null;
        if (username != null && !"anonymousUser".equals(username)) {
            socio = socioService.buscarPorUsername(username).orElse(null);
        }
        if (socio == null && dni != null && !dni.isBlank()) {
            socio = socioService.buscarPorDni(dni).orElse(null);
        }

        String nombreSocio = (socio != null) ? (socio.getNombre() + " " + socio.getApellido()).trim() : "Carlos Mendoza Pérez";
        String dniSocio = (socio != null) ? socio.getDni() : "72563123";
        String codigoSocio = (socio != null) ? ("#SOC-00" + socio.getId()) : "#SOC-001";

        Optional<Membresia> memOpt = (username != null) ? membresiaService.obtenerMembresiaActivaPorUsername(username) : Optional.empty();
        boolean activa = memOpt.isPresent() || socio != null;

        String horaActual = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"));

        if (activa) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "status", "CONCEDIDO",
                    "mensaje", "¡Acceso permitido! Torniquete 01 liberado.",
                    "socio", nombreSocio,
                    "dni", dniSocio,
                    "codigo", codigoSocio,
                    "sede", "Sede Las Am�ricas, Ate - Ingreso Central",
                    "hora", horaActual,
                    "visitasNuevas", 19
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "status", "DENEGADO",
                    "mensaje", "Membresía no vigente o suspendida. Por favor acérquese a recepción.",
                    "socio", nombreSocio,
                    "dni", dniSocio,
                    "sede", "Sede Las Am�ricas, Ate"
            ));
        }
    }

    // --- GESTIÓN DE MEMBRESÍAS PARA ADMINISTRADOR ---
    @GetMapping("/admin/membresias")
    public String membresiasAdmin(Model model) {
        model.addAttribute("membresias", membresiaService.listarTodas());
        model.addAttribute("socios", socioService.listarTodos());
        model.addAttribute("planes", planService.listarActivos());
        return "admin/membresias";
    }

    @PostMapping("/admin/membresias/asignar")
    public String asignarMembresiaAdmin(@Valid @ModelAttribute("asignacionDTO") MembresiaAsignacionDTO dto,
                                       BindingResult result,
                                       RedirectAttributes flash) {
        if (result.hasErrors()) {
            flash.addFlashAttribute("mensajeError", result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/membresias";
        }

        try {
            membresiaService.asignarPlanASocio(dto.getSocioId(), dto.getPlanId(), dto.getFechaInicio());
            flash.addFlashAttribute("mensajeExito", "Membresía asignada exitosamente.");
        } catch (Exception e) {
            flash.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/admin/membresias";
    }
}
