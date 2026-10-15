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
import pe.edu.utp.Grupo04.dto.PromocionDTO;
import pe.edu.utp.Grupo04.model.Promocion;
import pe.edu.utp.Grupo04.service.PromocionService;

@Controller
public class PromocionController {

    private final PromocionService promocionService;

    public PromocionController(PromocionService promocionService) {
        this.promocionService = promocionService;
    }

    // --- VISTA PÚBLICA DE PROMOCIONES ---
    @GetMapping("/promociones")
    public String promocionesPublico(Model model) {
        model.addAttribute("promociones", promocionService.listarVigentes());
        return "public/promociones";
    }

    // --- GESTIÓN ADMINISTRATIVA DE PROMOCIONES ---
    @GetMapping("/admin/promociones")
    public String promocionesAdmin(Model model) {
        model.addAttribute("promociones", promocionService.listarTodas());
        return "admin/promociones";
    }

    @PostMapping("/admin/promociones/guardar")
    public String guardarPromocionAdmin(@Valid @ModelAttribute("promocionDTO") PromocionDTO dto,
                                        BindingResult result,
                                        RedirectAttributes flash) {
        if (result.hasErrors()) {
            flash.addFlashAttribute("mensajeError", result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/promociones";
        }

        try {
            Promocion promocion = new Promocion();
            if (dto.getId() != null) {
                promocion.setId(dto.getId());
            }
            promocion.setTitulo(dto.getTitulo());
            promocion.setDescripcion(dto.getDescripcion());
            promocion.setPorcentajeDescuento(dto.getPorcentajeDescuento());
            promocion.setFechaInicio(dto.getFechaInicio());
            promocion.setFechaFin(dto.getFechaFin());
            promocion.setActiva(dto.getActiva() != null ? dto.getActiva() : true);

            promocionService.guardar(promocion);
            flash.addFlashAttribute("mensajeExito", "Promoción guardada exitosamente.");
        } catch (Exception e) {
            flash.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/admin/promociones";
    }

    @PostMapping("/admin/promociones/cambiar-estado")
    public String cambiarEstadoPromocionAdmin(@RequestParam Long id,
                                             @RequestParam boolean activa,
                                             RedirectAttributes flash) {
        try {
            promocionService.cambiarEstado(id, activa);
            flash.addFlashAttribute("mensajeExito", "Estado de la promoción actualizado con éxito.");
        } catch (Exception e) {
            flash.addFlashAttribute("mensajeError", "Error al actualizar estado: " + e.getMessage());
        }
        return "redirect:/admin/promociones";
    }
}
