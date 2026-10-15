package pe.edu.utp.Grupo04.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.utp.Grupo04.dto.AfluenciaDTO;
import pe.edu.utp.Grupo04.service.AfluenciaService;

@Controller
public class AfluenciaController {

    private final AfluenciaService afluenciaService;

    public AfluenciaController(AfluenciaService afluenciaService) {
        this.afluenciaService = afluenciaService;
    }

    // --- VISTA PÚBLICA DE AFLUENCIA ---
    @GetMapping("/afluencia")
    public String afluenciaPublico(Model model) {
        model.addAttribute("afluencia", afluenciaService.obtenerAfluenciaVigente());
        model.addAttribute("ultimaActualizacion", afluenciaService.obtenerUltimaFechaActualizacion());
        return "public/afluencia";
    }

    // --- VISTA DE AFLUENCIA PARA CLIENTE ---
    @GetMapping("/cliente/afluencia")
    public String afluenciaCliente(Model model) {
        model.addAttribute("afluencia", afluenciaService.obtenerAfluenciaVigente());
        model.addAttribute("ultimaActualizacion", afluenciaService.obtenerUltimaFechaActualizacion());
        return "cliente/afluencia";
    }

    // --- GESTIÓN DE AFLUENCIA PARA ADMINISTRADOR ---
    @GetMapping("/admin/afluencia")
    public String afluenciaAdmin(Model model) {
        model.addAttribute("registros", afluenciaService.listarTodos());
        model.addAttribute("afluenciaVigente", afluenciaService.obtenerAfluenciaVigente());
        model.addAttribute("ultimaActualizacion", afluenciaService.obtenerUltimaFechaActualizacion());
        return "admin/afluencia";
    }

    @PostMapping("/admin/afluencia/registrar")
    public String registrarAfluenciaAdmin(@Valid @ModelAttribute("afluenciaDTO") AfluenciaDTO afluenciaDTO,
                                         BindingResult result,
                                         RedirectAttributes flash) {
        if (result.hasErrors()) {
            flash.addFlashAttribute("mensajeError", result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/afluencia";
        }

        try {
            afluenciaService.registrarOActualizar(afluenciaDTO.getFecha(),
                    afluenciaDTO.getFranjaHoraria(),
                    afluenciaDTO.getConcurrenciaAprox());
            flash.addFlashAttribute("mensajeExito", "Registro de afluencia guardado exitosamente.");
        } catch (Exception e) {
            flash.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/admin/afluencia";
    }
}
