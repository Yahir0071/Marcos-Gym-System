package pe.edu.utp.Grupo04.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.utp.Grupo04.model.enums.EstadoSolicitudCongelamiento;
import pe.edu.utp.Grupo04.service.CongelamientoService;

@Controller
@RequestMapping("/admin/membresias/congelamientos")
public class CongelamientoAdminController {

    private final CongelamientoService congelamientos;

    public CongelamientoAdminController(CongelamientoService congelamientos) {
        this.congelamientos = congelamientos;
    }

    @GetMapping
    public String listar(
            @RequestParam(defaultValue = "PENDIENTE") EstadoSolicitudCongelamiento estado,
            @RequestParam(defaultValue = "0") int pagina, Model model) {
        model.addAttribute("estado", estado);
        model.addAttribute("estados", EstadoSolicitudCongelamiento.values());
        model.addAttribute("solicitudes", congelamientos.listarParaRevision(estado, Math.clamp(pagina, 0, 10_000)));
        return "admin/congelamientos";
    }

    @PostMapping("/{id}/resolver")
    public String resolver(
            @PathVariable Long id, @RequestParam EstadoSolicitudCongelamiento decision,
            Authentication usuario, RedirectAttributes flash) {
        try {
            congelamientos.resolver(id, decision, usuario.getName());
            flash.addFlashAttribute("mensajeExito", "Solicitud actualizada.");
        } catch (IllegalArgumentException | IllegalStateException excepcion) {
            flash.addFlashAttribute("mensajeError", excepcion.getMessage());
        }
        return "redirect:/admin/membresias/congelamientos";
    }
}
