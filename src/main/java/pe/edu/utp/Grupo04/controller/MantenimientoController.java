package pe.edu.utp.Grupo04.controller;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pe.edu.utp.Grupo04.dto.MantenimientoDTO;
import pe.edu.utp.Grupo04.model.Mantenimiento;
import pe.edu.utp.Grupo04.model.enums.EstadoEquipo;
import pe.edu.utp.Grupo04.service.EquipoService;
import pe.edu.utp.Grupo04.service.MantenimientoService;

@Controller
public class MantenimientoController {

    private final MantenimientoService mantenimientoService;
    private final EquipoService equipoService;

    public MantenimientoController(MantenimientoService mantenimientoService, EquipoService equipoService) {
        this.mantenimientoService = mantenimientoService;
        this.equipoService = equipoService;
    }

    @GetMapping("/admin/mantenimientos")
    public String mantenimientosAdmin(Model model) {
        model.addAttribute("mantenimientos", mantenimientoService.listarTodos());
        model.addAttribute("equipos", equipoService.listarTodos());
        return "admin/mantenimientos";
    }

    @PostMapping("/admin/mantenimientos/registrar")
    public String registrarMantenimientoAdmin(@Valid @ModelAttribute("mantenimientoDTO") MantenimientoDTO dto,
                                             BindingResult result,
                                             RedirectAttributes flash) {
        if (result.hasErrors()) {
            flash.addFlashAttribute("mensajeError", result.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/admin/mantenimientos";
        }

        try {
            Mantenimiento m = new Mantenimiento();
            m.setFechaMantenimiento(dto.getFechaMantenimiento());
            m.setTipo(dto.getTipo());
            m.setDescripcion(dto.getDescripcion());
            m.setObservaciones(dto.getObservaciones());

            mantenimientoService.registrarMantenimiento(dto.getEquipoId(), m);
            flash.addFlashAttribute("mensajeExito", "Intervención de mantenimiento registrada con éxito.");
        } catch (Exception e) {
            flash.addFlashAttribute("mensajeError", e.getMessage());
        }
        return "redirect:/admin/mantenimientos";
    }

    @GetMapping("/admin/reporte-mantenimiento")
    public String reporteMantenimientoAdmin(Model model) {
        model.addAttribute("mantenimientos", mantenimientoService.listarTodos());
        model.addAttribute("equipos", equipoService.listarTodos());
        model.addAttribute("equiposEnMantenimiento", equipoService.listarPorEstado(EstadoEquipo.EN_MANTENIMIENTO));
        model.addAttribute("equiposFueraDeServicio", equipoService.listarPorEstado(EstadoEquipo.FUERA_DE_SERVICIO));
        return "admin/reporte-mantenimiento";
    }
}
