package pe.edu.utp.Grupo04.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.utp.Grupo04.dto.SocioRegistroDTO;
import pe.edu.utp.Grupo04.model.Socio;
import pe.edu.utp.Grupo04.service.PlanService;
import pe.edu.utp.Grupo04.service.SocioService;

@Controller
public class SocioController {

    private final SocioService socioService;
    private final PlanService planService;

    public SocioController(SocioService socioService, PlanService planService) {
        this.socioService = socioService;
        this.planService = planService;
    }

    @GetMapping("/admin/socios")
    public String sociosAdmin(Model model) {
        model.addAttribute("socios", socioService.listarTodos());
        model.addAttribute("planes", planService.listarActivos());
        return "admin/socios";
    }

    @PostMapping("/admin/socios/guardar")
    public String guardarSocioAdmin(@Valid @ModelAttribute("socioRegistroDTO") SocioRegistroDTO dto,
                                    BindingResult result,
                                    RedirectAttributes flash) {
        if (result.hasErrors()) {
            flash.addFlashAttribute("mensajeError", result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/socios";
        }

        try {
            Socio socio = new Socio();
            socio.setDni(dto.getDni());
            socio.setNombres(dto.getNombres());
            socio.setApellidos(dto.getApellidos());
            socio.setTelefono(dto.getTelefono());

            socioService.registrarSocio(socio, dto.getEmail(), dto.getPassword());
            flash.addFlashAttribute("mensajeExito", "Socio y usuario registrados exitosamente.");
        } catch (Exception e) {
            flash.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/admin/socios";
    }
}
