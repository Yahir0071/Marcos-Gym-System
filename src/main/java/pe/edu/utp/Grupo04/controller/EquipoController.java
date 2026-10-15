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
import pe.edu.utp.Grupo04.dto.EquipoDTO;
import pe.edu.utp.Grupo04.model.Equipo;
import pe.edu.utp.Grupo04.model.enums.EstadoEquipo;
import pe.edu.utp.Grupo04.service.EquipoService;

@Controller
public class EquipoController {

    private final EquipoService equipoService;

    public EquipoController(EquipoService equipoService) {
        this.equipoService = equipoService;
    }

    @GetMapping("/admin/equipos")
    public String equiposAdmin(Model model) {
        model.addAttribute("equipos", equipoService.listarTodos());
        return "admin/equipos";
    }

    @PostMapping("/admin/equipos/guardar")
    public String guardarEquipoAdmin(@Valid @ModelAttribute("equipoDTO") EquipoDTO equipoDTO,
                                     BindingResult result,
                                     RedirectAttributes flash) {
        if (result.hasErrors()) {
            flash.addFlashAttribute("mensajeError", result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/equipos";
        }

        try {
            Equipo equipo = new Equipo();
            if (equipoDTO.getId() != null) {
                equipo.setId(equipoDTO.getId());
            }
            equipo.setNombre(equipoDTO.getNombre());
            equipo.setCodigo(equipoDTO.getCodigo());
            equipo.setEstado(equipoDTO.getEstado());

            equipoService.guardar(equipo);
            flash.addFlashAttribute("mensajeExito", "Equipo registrado exitosamente.");
        } catch (Exception e) {
            flash.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/admin/equipos";
    }

    @PostMapping("/admin/equipos/cambiar-estado")
    public String cambiarEstadoEquipoAdmin(@RequestParam Long id,
                                          @RequestParam EstadoEquipo estado,
                                          RedirectAttributes flash) {
        try {
            equipoService.cambiarEstado(id, estado);
            flash.addFlashAttribute("mensajeExito", "Estado del equipo actualizado a " + estado.name());
        } catch (Exception e) {
            flash.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/admin/equipos";
    }
}
