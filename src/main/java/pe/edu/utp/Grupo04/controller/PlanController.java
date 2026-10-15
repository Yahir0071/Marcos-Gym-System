package pe.edu.utp.Grupo04.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.utp.Grupo04.dto.PlanDTO;
import pe.edu.utp.Grupo04.model.Plan;
import pe.edu.utp.Grupo04.service.PlanService;

@Controller
public class PlanController {

    private final PlanService planService;

    public PlanController(PlanService planService) {
        this.planService = planService;
    }

    // --- VISTA PÚBLICA DE PLANES ---
    @GetMapping("/planes")
    public String planesPublico(Model model) {
        model.addAttribute("planes", planService.listarActivos());
        return "public/planes";
    }

    // --- GESTIÓN ADMINISTRATIVA DE PLANES ---
    @GetMapping("/admin/planes")
    public String planesAdmin(Model model) {
        model.addAttribute("planes", planService.listarTodos());
        return "admin/planes";
    }

    @PostMapping("/admin/planes/guardar")
    public String guardarPlanAdmin(@Valid @ModelAttribute("planDTO") PlanDTO planDTO,
                                   BindingResult result,
                                   RedirectAttributes flash) {
        if (result.hasErrors()) {
            flash.addFlashAttribute("mensajeError", result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/planes";
        }

        Plan plan = new Plan();
        if (planDTO.getId() != null) {
            plan.setId(planDTO.getId());
        }
        plan.setNombre(planDTO.getNombre());
        plan.setDescripcion(planDTO.getDescripcion());
        plan.setDuracionDias(planDTO.getDuracionDias());
        plan.setPrecio(planDTO.getPrecio());
        plan.setActivo(planDTO.getActivo() != null ? planDTO.getActivo() : true);

        planService.guardar(plan);
        flash.addFlashAttribute("mensajeExito", "Plan guardado exitosamente.");
        return "redirect:/admin/planes";
    }

    @PostMapping("/admin/planes/cambiar-estado")
    public String cambiarEstadoPlanAdmin(@RequestParam("id") Long id,
                                         @RequestParam("activo") boolean activo,
                                         RedirectAttributes flash) {
        try {
            planService.cambiarEstado(id, activo);
            flash.addFlashAttribute("mensajeExito", "Estado del plan actualizado exitosamente.");
        } catch (Exception e) {
            flash.addFlashAttribute("mensajeError", "Error al actualizar estado del plan: " + e.getMessage());
        }
        return "redirect:/admin/planes";
    }
}
