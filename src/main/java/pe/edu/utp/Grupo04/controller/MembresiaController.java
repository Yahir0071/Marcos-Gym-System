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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.utp.Grupo04.dto.MembresiaAsignacionDTO;
import pe.edu.utp.Grupo04.model.Membresia;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.service.AccesoService;
import pe.edu.utp.Grupo04.service.MembresiaService;
import pe.edu.utp.Grupo04.service.CongelamientoService;
import pe.edu.utp.Grupo04.service.PlanService;
import pe.edu.utp.Grupo04.service.SocioService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;

@Controller
public class MembresiaController {

    private static final ZoneId ZONA_GIMNASIO = ZoneId.of("America/Lima");
    private final MembresiaService membresiaService;
    private final CongelamientoService congelamientoService;
    private final SocioService socioService;
    private final PlanService planService;
    private final AccesoService accesoService;

    public MembresiaController(MembresiaService membresiaService,
                               SocioService socioService,
                               PlanService planService,
                               CongelamientoService congelamientoService,
                               AccesoService accesoService) {
        this.membresiaService = membresiaService;
        this.socioService = socioService;
        this.planService = planService;
        this.congelamientoService = congelamientoService;
        this.accesoService = accesoService;
    }

    // --- CONSULTA DE MEMBRESÍA PARA CLIENTE ---
    @GetMapping("/cliente/mi-membresia")
    public String miMembresiaCliente(Authentication authentication, Model model) {
        String username = authentication.getName();
        LocalDate hoy = LocalDate.now(ZONA_GIMNASIO);
        Optional<Socio> socioOpt = socioService.buscarPorUsername(username);
        Optional<Membresia> membresiaOpt = membresiaService.obtenerMembresiaActivaPorUsername(username);

        socioOpt.ifPresent(socio -> model.addAttribute("socio", socio));
        model.addAttribute("fechaMinimaPausa", hoy.toString());

        if (membresiaOpt.isPresent()) {
            Membresia membresia = membresiaOpt.get();
            long diasRestantes = membresiaService.calcularDiasRestantes(membresia);
            boolean porVencer = membresiaService.estaPorVencer(membresia, 7);

            model.addAttribute("membresia", membresia);
            model.addAttribute("membresiaCongelada", congelamientoService.membresiaCongelada(
                    membresia.getId(), hoy));
            model.addAttribute("diasRestantes", diasRestantes);
            model.addAttribute("estaPorVencer", porVencer);
        } else {
            model.addAttribute("membresia", null);
            model.addAttribute("diasRestantes", 0);
            model.addAttribute("estaPorVencer", false);
        }

        return "cliente/mi-membresia";
    }

    // --- C06: VALIDACIÓN DE TORNIQUETE Y CONTROL DE ACCESO + AFLUENCIA ---
    @GetMapping("/cliente/api/carnet/qr")
    @ResponseBody
    public ResponseEntity<?> generarPaseQr(Authentication authentication) {
        String username = (authentication != null && authentication.isAuthenticated()) ? authentication.getName() : null;
        return ResponseEntity.ok(accesoService.generarPaseQr(username));
    }

    @PostMapping("/cliente/api/torniquete/validar")
    @ResponseBody
    public ResponseEntity<?> validarAccesoTorniquete(Authentication authentication,
                                                     @RequestBody(required = false) Map<String, String> body) {
        String username = (authentication != null && authentication.isAuthenticated()) ? authentication.getName() : null;
        String token = body != null ? body.get("token") : null;
        return ResponseEntity.ok(accesoService.validarAcceso(username, token));
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
