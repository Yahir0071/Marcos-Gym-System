package pe.edu.utp.Grupo04.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.utp.Grupo04.dto.PagoDTO;
import pe.edu.utp.Grupo04.service.MembresiaService;
import pe.edu.utp.Grupo04.service.PagoService;

@Controller
public class PagoController {

    private final PagoService pagoService;
    private final MembresiaService membresiaService;

    public PagoController(PagoService pagoService, MembresiaService membresiaService) {
        this.pagoService = pagoService;
        this.membresiaService = membresiaService;
    }

    @GetMapping("/admin/pagos")
    public String pagosAdmin(Model model) {
        model.addAttribute("pagos", pagoService.listarTodos());
        model.addAttribute("membresias", membresiaService.listarTodas());
        return "admin/pagos";
    }

    @PostMapping("/admin/pagos/registrar")
    public String registrarPagoAdmin(@Valid @ModelAttribute("pagoDTO") PagoDTO pagoDTO,
                                     BindingResult result,
                                     RedirectAttributes flash) {
        if (result.hasErrors()) {
            flash.addFlashAttribute("mensajeError", result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/pagos";
        }

        try {
            pagoService.registrarPagoPresencial(pagoDTO.getMembresiaId(), pagoDTO.getMonto(), pagoDTO.getMetodoPago());
            flash.addFlashAttribute("mensajeExito", "Pago registrado y membresía actualizada.");
        } catch (Exception e) {
            flash.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/admin/pagos";
    }
}
